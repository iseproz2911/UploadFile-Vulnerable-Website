package site;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class FileUploadServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);

        if (isMultipart) {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);

            try {
                List items = upload.parseRequest(request);
                Iterator iterator = items.iterator();
                while (iterator.hasNext()) {
                    FileItem item = (FileItem) iterator.next();
                    if (!item.isFormField()) {
                        String fileName = new File(item.getName()).getName();
                        String sanitizedFileName = sanitizeFileName(fileName);

                        String tempDirectory = getServletContext().getRealPath("/") + File.separator + "tmp"
                                + File.separator + sanitizedFileName;
                        File tempFile = new File(tempDirectory);
                        System.out.println("Đường dẫn thư mục tạm: " + tempDirectory);
                        item.write(tempFile);

                        // Kiểm tra đuôi file
                        if (fileName.toLowerCase().endsWith(".zip")) {
                            String randomDirectoryName = generateRandomDirectoryName();
                            File destinationPath = new File(
                                    getServletContext().getRealPath("/") + File.separator + "uploads" + File.separator
                                            + randomDirectoryName);
                            if (!destinationPath.exists()) {
                                boolean status = destinationPath.mkdirs();
                            }

                            // Giải nén file trước khi kiểm tra và lưu
                            unzipAndSaveFiles(tempFile, destinationPath);
                            System.out.println("File saved to: " + destinationPath.getAbsolutePath());

                            // Xóa file zip 
                            tempFile.delete();

                            // Lọc và xóa các file 
                            filterFiles(destinationPath);

                            response.getWriter().println("<html><body>");
                            response.getWriter().println("<h2>Unzip and Filter successfully</h2>");
                            response.getWriter().println("<p>Access the files here: <a href=\"" + request.getContextPath()
                                    + "/uploads/" + randomDirectoryName + "\" target=\"_blank\">" + randomDirectoryName
                                    + "</a></p>");
                            response.getWriter().println("<p><a href=\"" + request.getContextPath()
                                    + "\">Upload another file</a></p>");
                            response.getWriter().println("</body></html>");
                            response.getWriter().flush();
                        } else {
                            // File không phải là file ZIP, xóa file temp và thông báo lỗi
                            tempFile.delete();
                            response.getWriter().println("<html><body>");
                            response.getWriter().println("<h2>File upload failed</h2>");
                            response.getWriter().println("<p>Only .zip files are allowed.</p>");
                            response.getWriter().println("<p><a href=\"" + request.getContextPath()
                                    + "\">Upload another file</a></p>");
                            response.getWriter().println("</body></html>");
                            response.getWriter().flush();
                        }
                    }
                }
            } catch (FileUploadException e) {
                e.printStackTrace();
                response.sendRedirect(request.getContextPath());
            } catch (Exception e) {
                e.printStackTrace();
                response.sendRedirect(request.getContextPath());
            }
        } else {
            response.sendRedirect(request.getContextPath());
        }
    }

    private void filterFiles(File directory) {
        File[] files = directory.listFiles();
        for (File file : files) {
            if (!isAllowedFileType(file)) {
                file.delete();
            }
        }
    }

    private boolean isAllowedFileType(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".docx") || fileName.endsWith(".txt") || fileName.endsWith(".png")
                || fileName.endsWith(".jpg") || fileName.endsWith(".pdf");
    }
//Block Path Traversal
    private String sanitizeFileName(String originalFileName) {
        String sanitizedFileName = originalFileName.replaceAll("[/\\\\.,]", "_");
        return sanitizedFileName;
    }
//Random folder
    private String generateRandomDirectoryName() {
        Random random = new Random();
        return "29112002" + random.nextInt(100);
    }
//Unzip
    private void unzipAndSaveFiles(File zipFile, File destination) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                File entryFile = new File(destination, entryName);
                try (FileOutputStream fos = new FileOutputStream(entryFile)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
            }
        }
    }
}
