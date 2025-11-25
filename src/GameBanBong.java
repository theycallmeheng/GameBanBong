import javax.swing.SwingUtilities;
import javax.swing.JFrame;

public class GameBanBong {
    public static void main(String[] args){
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame f = new JFrame("Ban Bong ULTRA ✈️");
                GamePanel p = new GamePanel();
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setResizable(false);
                f.add(p);
                f.pack();
                f.setLocationRelativeTo(null);
                f.setVisible(true);
                p.requestFocusInWindow();
            }
        });
    }
}