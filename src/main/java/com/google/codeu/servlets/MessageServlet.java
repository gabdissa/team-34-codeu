/*
 * Copyright 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.codeu.servlets;

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.codeu.data.Datastore;
import com.google.codeu.data.Message;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.Document.Type;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.Sentiment;
import com.google.codeu.data.User;
import java.util.UUID;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;
import java.io.ByteArrayOutputStream;


/** Handles fetching and saving {@link Message} instances. */
@WebServlet("/messages")
public class MessageServlet extends HttpServlet {

  private Datastore datastore;

  @Override
  public void init() {
    datastore = new Datastore();
  }

  /**
   * Responds with a JSON representation of {@link Message} data for a specific user. Responds with
   * an empty array if the user is not provided.
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    response.setContentType("application/json");

    String user = request.getParameter("user");

    if (user == null || user.equals("")) {
      // Request is invalid, return empty array
      response.getWriter().println("[]");
      return;
    }

    List<Message> messages = datastore.getMessages(user);
    Gson gson = new Gson();
    String json = gson.toJson(messages);

    response.getWriter().println(json);
  }

  /** Stores a new {@link Message}. */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

    UserService userService = UserServiceFactory.getUserService();
    if (!userService.isUserLoggedIn()) {
      response.sendRedirect("/index.html");
      return;
    }

    String user = userService.getCurrentUser().getEmail();
    String userText = Jsoup.clean(request.getParameter("text"), Whitelist.basicWithImages());

    //Gets the Url of the Image that the use uplaoded
    BlobKey blobKey = getBlobKey(request, "image");

    String imageUrl = "";
    String imageTags = "";
    if(blobKey != null)
    {
       imageUrl = getUploadedFileUrl(blobKey);

      // Get the labels of the image that the user uploaded.
      byte[] blobBytes = getBlobBytes(blobKey);
      List<EntityAnnotation> imageLabels = getImageLabels(blobBytes);

      //Adds Labes as a string
      imageTags = "Tags: ";
      for(EntityAnnotation label : imageLabels){
        imageTags = imageTags + " " + label.getDescription();
      }
    }

    else{
      imageUrl = null;
      imageTags = "Tags: No Image Uploaded";
    }





    Document doc = Document.newBuilder().setContent(userText).setType(Document.Type.PLAIN_TEXT).build();
    LanguageServiceClient languageService = LanguageServiceClient.create();
    Sentiment sentiment = languageService.analyzeSentiment(doc).getDocumentSentiment();
    double score = sentiment.getScore();
    languageService.close();

    //Checks if the a url was uploaded
    if(imageUrl != null){
      userText = userText +"<img src=\""  + imageUrl + "\">";
    }

    //Formats a image url into an Image tag if the user enters one into the text field
    String regex = "(https?://\\S+\\.(png|jpg))";
    String replacement = "<img src=\"$1\" />";
    String textWithImagesReplaced = userText.replaceAll(regex, replacement);

    Message message = new Message(UUID.randomUUID(), user,textWithImagesReplaced, System.currentTimeMillis(), score, imageTags);
    datastore.storeMessage(message);

    // System.out.println("The score is " + score);
    response.sendRedirect("/user-page.html?user=" + user);
  }

  // /**
  //  * Returns a URL that points to the uploaded file, or null if the user didn't upload a file.
  //  */
  // private String getUploadedFileUrl(HttpServletRequest request, String formInputElementName){
  //   BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
  //   Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
  //   List<BlobKey> blobKeys = blobs.get("image");
  //
  //   // User submitted form without selecting a file, so we can't get a URL. (devserver)
  //   if(blobKeys == null || blobKeys.isEmpty()) {
  //     return null;
  //   }
  //
  //   // Our form only contains a single file input, so get the first index.
  //   BlobKey blobKey = blobKeys.get(0);
  //
  //   // User submitted form without selecting a file, so we can't get a URL. (live server)
  //   BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
  //   if (blobInfo.getSize() == 0) {
  //     blobstoreService.delete(blobKey);
  //     return null;
  //   }
  //
  //   // We could check the validity of the file here, e.g. to make sure it's an image file
  //   // https://stackoverflow.com/q/10779564/873165
  //
  //   // Use ImagesService to get a URL that points to the uploaded file.
  //   ImagesService imagesService = ImagesServiceFactory.getImagesService();
  //   ServingUrlOptions options = ServingUrlOptions.Builder.withBlobKey(blobKey);
  //   return imagesService.getServingUrl(options);
  // }
  /**
   * Returns the BlobKey that points to the file uploaded by the user, or null if the user didn't upload a file.
   */
  private BlobKey getBlobKey(HttpServletRequest request, String formInputElementName){
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
    List<BlobKey> blobKeys = blobs.get("image");

    // User submitted form without selecting a file, so we can't get a BlobKey. (devserver)
    if(blobKeys == null || blobKeys.isEmpty()) {
      return null;
    }

    // Our form only contains a single file input, so get the first index.
    BlobKey blobKey = blobKeys.get(0);

    // User submitted form without selecting a file, so the BlobKey is empty. (live server)
    BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
    if (blobInfo.getSize() == 0) {
      blobstoreService.delete(blobKey);
      return null;
    }

    return blobKey;
  }

  /**
   * Blobstore stores files as binary data. This function retrieves the
   * binary data stored at the BlobKey parameter.
   */
  private byte[] getBlobBytes(BlobKey blobKey) throws IOException {
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();

    int fetchSize = BlobstoreService.MAX_BLOB_FETCH_SIZE;
    long currentByteIndex = 0;
    boolean continueReading = true;
    while (continueReading) {
      // end index is inclusive, so we have to subtract 1 to get fetchSize bytes
      byte[] b = blobstoreService.fetchData(blobKey, currentByteIndex, currentByteIndex + fetchSize - 1);
      outputBytes.write(b);

      // if we read fewer bytes than we requested, then we reached the end
      if (b.length < fetchSize) {
        continueReading = false;
      }

      currentByteIndex += fetchSize;
    }

    return outputBytes.toByteArray();
  }

  /**
   * Uses the Google Cloud Vision API to generate a list of labels that apply to the image
   * represented by the binary data stored in imgBytes.
   */
  private List<EntityAnnotation> getImageLabels(byte[] imgBytes) throws IOException {
    ByteString byteString = ByteString.copyFrom(imgBytes);
    Image image = Image.newBuilder().setContent(byteString).build();

    Feature feature = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
    AnnotateImageRequest request =
        AnnotateImageRequest.newBuilder().addFeatures(feature).setImage(image).build();
    List<AnnotateImageRequest> requests = new ArrayList<>();
    requests.add(request);

    ImageAnnotatorClient client = ImageAnnotatorClient.create();
    BatchAnnotateImagesResponse batchResponse = client.batchAnnotateImages(requests);
    client.close();
    List<AnnotateImageResponse> imageResponses = batchResponse.getResponsesList();
    AnnotateImageResponse imageResponse = imageResponses.get(0);

    if (imageResponse.hasError()) {
      System.err.println("Error getting image labels: " + imageResponse.getError().getMessage());
      return null;
    }

    return imageResponse.getLabelAnnotationsList();
  }

  /**
   * Returns a URL that points to the uploaded file.
   */
  private String getUploadedFileUrl(BlobKey blobKey){
    ImagesService imagesService = ImagesServiceFactory.getImagesService();
    ServingUrlOptions options = ServingUrlOptions.Builder.withBlobKey(blobKey);
    return imagesService.getServingUrl(options);
  }

}
