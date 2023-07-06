package searchengine.parsers;


import lombok.Getter;

import java.util.concurrent.CopyOnWriteArrayList;

public class NodeUrl {
    private volatile NodeUrl parent;
    private final String urlPage;
    @Getter
    private final CopyOnWriteArrayList<NodeUrl> children;

    public NodeUrl(String urlPage) {
        this.urlPage = urlPage;
        parent = null;
        children = new CopyOnWriteArrayList<>();
    }

    public synchronized void addChild(NodeUrl element) {
        NodeUrl root = getRootElement();
        if (!root.contains(element.getUrl())) {
            element.setParent(this);
            children.add(element);
        }
    }

    private boolean contains(String url) {
        if (this.urlPage.equals(url)) {
            return true;
        }
        for (NodeUrl child : children) {
            if (child.contains(url))
                return true;
        }
        return false;
    }

    public String getUrl() {
        return urlPage;
    }

    private void setParent(NodeUrl nodeUrl) {
        synchronized (this) {
            this.parent = nodeUrl;
        }
    }

    public NodeUrl getRootElement() {
        return parent == null ? this : parent.getRootElement();
    }

}
