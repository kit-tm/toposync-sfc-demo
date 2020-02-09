import view.DemoUI;

import java.io.IOException;

public class Main {


    public static void main(String[] args) throws IOException {
        System.setProperty("org.graphstream.ui", "swing"); // tells GraphStream to use Swing

        new DemoUI();
    }
}
