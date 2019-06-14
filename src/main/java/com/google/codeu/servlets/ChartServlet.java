@WebServlet("/bookchart")
public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException{
        response.setContentType("application/json");
        response.getWriter().println("slowly but surely");
        }

