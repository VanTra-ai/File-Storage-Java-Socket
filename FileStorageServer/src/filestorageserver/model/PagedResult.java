package filestorageserver.model;

import java.util.List;

public class PagedResult<T> {

    private final List<T> items;
    private final long totalItems;
    private final int totalPages;
    private final int currentPage;
    private final int pageSize;

    public PagedResult(List<T> items, long totalItems, int currentPage, int pageSize) {
        this.items = items;
        this.totalItems = totalItems;
        this.pageSize = pageSize;
        this.currentPage = currentPage;

        final int calculatedPages; // Biến tạm để tính toán
        if (totalItems <= 0) {
            calculatedPages = 0; // Không có item thì không có trang
        } else if (pageSize <= 0) {
            calculatedPages = 1; // Nếu pageSize không hợp lệ nhưng có item, coi như có 1 trang
        } else {
            // Tính toán bình thường
            calculatedPages = (int) Math.ceil((double) totalItems / pageSize);
        }
        this.totalPages = calculatedPages; // Gán giá trị MỘT LẦN cho biến final
    }

    public List<T> getItems() {
        return items;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }
}
