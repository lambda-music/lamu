package lamu.utils.lib;

import javax.swing.JTextField;
import javax.swing.text.Document;

public class JPulsarTextField extends JTextField implements JUserObjectContainer {

    public JPulsarTextField() {
    }

    public JPulsarTextField(String text) {
        super(text);
    }

    public JPulsarTextField(int columns) {
        super(columns);
    }

    public JPulsarTextField(String text, int columns) {
        super(text, columns);
    }

    public JPulsarTextField(Document doc, String text, int columns) {
        super(doc, text, columns);
    }
    volatile Object userObject=null;
    @Override
    public Object getUserObject() {
        return userObject;
    }
    @Override
    public void setUserObject(Object userObject) {
        this.userObject = userObject;
    }
    @Override
    public void setSelected(boolean selected) {
    }
    @Override
    public boolean isSelected() {
        return false;
    }
}
