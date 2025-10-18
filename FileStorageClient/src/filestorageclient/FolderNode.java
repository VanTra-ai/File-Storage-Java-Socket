package filestorageclient;

/**
 * Lớp này là một đối tượng tùy chỉnh để lưu trữ trong mỗi nút của JTree. Nó lưu
 * cả ID (để xử lý) và Tên (để hiển thị).
 */
public class FolderNode {

    private final int id;
    private final String name;

    public FolderNode(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Đây là phương thức quan trọng nhất! JTree sẽ tự động gọi .toString() để
     * quyết định văn bản nào sẽ hiển thị trên cây.
     */
    @Override
    public String toString() {
        return this.name;
    }
}
