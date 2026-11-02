package com.smartdining.servlet;

import java.io.IOException;
import com.smartdining.dao.ProductDAO;
import com.smartdining.dao.impl.ProductDAOImpl;
import com.smartdining.entity.TableShop;
import com.smartdining.enums.ProductStatus;
import com.smartdining.enums.TableStatus;
import com.smartdining.service.TableShopService;
import com.smartdining.service.impl.TableShopServiceImpl;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/home/index")
public class HomeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final TableShopService tableShopService = new TableShopServiceImpl();
    private final ProductDAO productDAO = new ProductDAOImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        TableShop table = findRequestedTable(request);
        if (table == null || table.getStatus() == TableStatus.INACTIVE) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "So ban hoac ma QR khong hop le");
            return;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute("currentTableId", table.getId());
        request.setAttribute("currentTableId", table.getId());

        request.setAttribute("products", productDAO.findAll().stream()
                .filter(product -> product.getStatus() == ProductStatus.AVAILABLE)
                .toList());
        com.smartdining.dao.CategoryDAO categoryDAO = new com.smartdining.dao.impl.CategoryDAOImpl();
        request.setAttribute("categories", categoryDAO.findAll());
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    private TableShop findRequestedTable(HttpServletRequest request) {
        String tableIdValue = request.getParameter("tableId");
        if (tableIdValue != null && !tableIdValue.isBlank()) {
            try {
                int tableId = Integer.parseInt(tableIdValue);
                return tableId > 0 ? tableShopService.findById(tableId) : null;
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return tableShopService.findByQrCodeUrl(request.getParameter("qr"));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
