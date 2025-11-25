import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;

/**
 * Game panel (đã tách từ file lớn)
 */
public class GamePanel extends JPanel implements ActionListener, KeyListener {

    // ===== Kích thước
    private static final int W = 800, H = 600;

    // ===== Trạng thái
    private static final int START=0, PLAYING=1, PAUSED=2, GAMEOVER=3;
    private int state = START;

    // ===== Máy bay
    private int px = W/2 - 30, py = H - 110;
    private int pW = 66, pH = 74;
    private int pSpeed = 7;
    private boolean left, right;

    // ===== Gameplay
    private List<Bullet> bullets = new ArrayList<Bullet>();
    private List<Ball> balls = new ArrayList<Ball>();
    private List<Particle> particles = new ArrayList<Particle>();
    private List<Trail> trails = new ArrayList<Trail>();
    private Random rnd = new Random();
    private int score = 0, lives = 3;

    // sao cho nền (parallax)
    private Star[] starsFar = new Star[130];
    private Star[] starsNear = new Star[70];

    // Loop
    private javax.swing.Timer timer = new javax.swing.Timer(16, this);

    public GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        setBackground(Color.BLACK);
        addKeyListener(this);
        initStars();
        resetAll();
    }

    private void initStars() {
        for (int i=0;i<starsFar.length;i++) starsFar[i] = new Star(rnd.nextInt(W), rnd.nextInt(H), 0.3f + rnd.nextFloat()*0.5f);
        for (int i=0;i<starsNear.length;i++) starsNear[i] = new Star(rnd.nextInt(W), rnd.nextInt(H), 0.8f + rnd.nextFloat()*1.2f);
    }

    private void resetAll() {
        bullets.clear(); balls.clear(); particles.clear(); trails.clear();
        score = 0; lives = 3; state = START;
        px = W/2 - pW/2; py = H - 110;
        for (int i=0;i<6;i++) spawnBall();
    }

    private void startPlay() {
        state = PLAYING;
        timer.start();
    }

    private void spawnBall() {
        int r = 22 + rnd.nextInt(20);
        int x = r + rnd.nextInt(W - 2*r);
        int y = 60 + rnd.nextInt(180);
        int vx = (rnd.nextBoolean()?1:-1) * (2 + rnd.nextInt(2));
        int vy = 2 + rnd.nextInt(2);
        Color c = new Color(120 + rnd.nextInt(120), 180 + rnd.nextInt(70), 255, 235);
        balls.add(new Ball(x,y,r,vx,vy,c));
    }

    private void shoot() {
        int bw=6, bh=18;
        int bx = px + pW/2 - bw/2;
        int by = py - 8;
        bullets.add(new Bullet(bx, by, bw, bh, 11));
        // lửa nòng súng nổ nhẹ
        spawnBurst(bx+bw/2, by, new Color(255,220,120,220), 10, 2.0f, 3.5f);
    }

    public void actionPerformed(ActionEvent e) {
        if (state == PLAYING) update();
        repaint();
    }

    private void update() {
        // Parallax starfield
        for (int i=0;i<starsFar.length;i++) { starsFar[i].y += starsFar[i].speed; if (starsFar[i].y >= H) starsFar[i] = new Star(rnd.nextInt(W), 0, starsFar[i].speed); }
        for (int i=0;i<starsNear.length;i++) { starsNear[i].y += starsNear[i].speed*1.5f; if (starsNear[i].y >= H) starsNear[i] = new Star(rnd.nextInt(W), 0, starsNear[i].speed); }

        // Plane move + trail
        int oldX = px;
        if (left)  px -= pSpeed;
        if (right) px += pSpeed;
        if (px < 0) px = 0;
        if (px + pW > W) px = W - pW;
        int dx = px - oldX;
        // trail mờ theo hướng bay
        trails.add(new Trail(px + pW/2, py + pH - 8, dx*0.5f, 2.5f + rnd.nextFloat()*1.5f, 0.9f));

        // Bullets
        for (int i=bullets.size()-1;i>=0;i--){
            Bullet b = bullets.get(i);
            b.y -= b.speed;
            if (b.y + b.h < 0) bullets.remove(i);
        }

        // Balls
        for (int i=0;i<balls.size();i++){
            Ball ball = balls.get(i);
            ball.x += ball.vx; ball.y += ball.vy;
            if (ball.x - ball.r < 0 || ball.x + ball.r > W) ball.vx = -ball.vx;
            if (ball.y - ball.r < 0) ball.vy = -ball.vy;

            // chạm máy bay -> mất mạng
            if (ball.y + ball.r >= py && ball.x + ball.r >= px && ball.x - ball.r <= px + pW) {
                loseLife();
                if (state != GAMEOVER) resetBall(ball);
            }
            if (ball.y - ball.r > H) resetBall(ball);
        }

        // Bullet vs Ball
        for (int i=bullets.size()-1;i>=0;i--){
            Bullet b = bullets.get(i); boolean hit=false;
            for (int j=0;j<balls.size();j++){
                Ball ball = balls.get(j);
                int cx = b.x + b.w/2, cy = b.y + b.h/2;
                int dx2 = cx - ball.x, dy2 = cy - ball.y;
                if (dx2*dx2 + dy2*dy2 <= ball.r*ball.r){
                    score += 10;
                    spawnExplosion(ball.x, ball.y, ball.color);
                    resetBall(ball);
                    hit = true; break;
                }
            }
            if (hit) bullets.remove(i);
        }

        // Particles
        for (int i=particles.size()-1;i>=0;i--){
            Particle p = particles.get(i);
            p.x += p.vx; p.y += p.vy;
            p.life -= 0.02f;
            if (p.life <= 0) particles.remove(i);
        }

        // Trails
        for (int i=trails.size()-1;i>=0;i--){
            Trail t = trails.get(i);
            t.y += t.vy; t.life -= 0.03f; t.size *= 0.98f;
            if (t.life <= 0) trails.remove(i);
        }
    }

    private void loseLife() {
        lives--;
        spawnExplosion(px + pW/2, py + pH/2, new Color(255,70,90,230));
        if (lives <= 0) { state = GAMEOVER; timer.stop(); }
    }

    private void resetBall(Ball ball){
        ball.r = 22 + rnd.nextInt(20);
        ball.x = ball.r + rnd.nextInt(W - 2*ball.r);
        ball.y = 50 + rnd.nextInt(160);
        ball.vx = (rnd.nextBoolean()?1:-1) * (2 + rnd.nextInt(2));
        ball.vy = 2 + rnd.nextInt(2);
        ball.color = new Color(120 + rnd.nextInt(120), 180 + rnd.nextInt(70), 255, 235);
    }

    private void spawnExplosion(int cx, int cy, Color base) {
        spawnBurst(cx, cy, base, 36, 2.5f, 4.0f);
        // halo sáng nhanh
        for (int i=0;i<12;i++){
            particles.add(new Particle(cx, cy, (rnd.nextFloat()-0.5f)*1.2f, (rnd.nextFloat()-0.5f)*1.2f,
                    new Color(255,255,255,160), 0.5f));
        }
    }

    private void spawnBurst(int cx,int cy, Color base, int count, float spMin, float spMax){
        for (int i=0;i<count;i++){
            double a = rnd.nextDouble()*Math.PI*2;
            float sp = spMin + rnd.nextFloat()*(spMax-spMin);
            Color c = new Color(
                    clamp(base.getRed()+rnd.nextInt(40),0,255),
                    clamp(base.getGreen()-rnd.nextInt(60),0,255),
                    clamp(base.getBlue(),0,255),
                    180);
            particles.add(new Particle(cx, cy, (float)Math.cos(a)*sp, (float)Math.sin(a)*sp, c, 1.0f));
        }
    }

    private int clamp(int v,int lo,int hi){ return v<lo?lo:(v>hi?hi:v); }

    // ===== Vẽ
    public void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        drawBackground(g2);

        // Máy bay + vật thể chỉ vẽ khi không ở START
        if (state != START) {
            drawTrails(g2);
            drawPlane(g2);
            drawBullets(g2);
            drawBalls(g2);
            drawParticles(g2);
            drawHUD(g2);
        }

        if (state == START) drawCenterBox(g2, "BAN BONG ULTRA", "ENTER: Bắt đầu  ·  ←/→: Di chuyển  ·  SPACE: Bắn  ·  P: Tạm dừng");
        else if (state == PAUSED) drawCenterBox(g2, "PAUSED", "Nhấn P để tiếp tục  ·  ENTER: Chơi mới");
        else if (state == GAMEOVER) drawCenterBox(g2, "GAME OVER", "Điểm: " + score + "  ·  ENTER: Chơi lại");
    }

    private void drawBackground(Graphics2D g2){
        // gradient trời đêm
        GradientPaint gp = new GradientPaint(0,0,new Color(8,12,28), 0,H,new Color(2,2,6));
        g2.setPaint(gp); g2.fillRect(0,0,W,H);

        // Parallax stars
        g2.setColor(new Color(255,255,255,90));
        for (int i=0;i<starsFar.length;i++){ g2.fillRect(starsFar[i].x, starsFar[i].y, 1,1); }
        g2.setColor(new Color(255,255,255,160));
        for (int i=0;i<starsNear.length;i++){ g2.fillRect(starsNear[i].x, starsNear[i].y, 1,1); }

        // Vignette
        RadialGradientPaint rg = new RadialGradientPaint(
                new Point2D.Float(W/2f, H/1.8f),
                Math.max(W, H),
                new float[]{0f, 1f},
                new Color[]{new Color(0,0,0,0), new Color(0,0,0,150)});
        g2.setPaint(rg);
        g2.fillRect(0,0,W,H);
    }

    private void drawBullets(Graphics2D g2){
        for (int i=0;i<bullets.size();i++){
            Bullet b = bullets.get(i);
            // glow
            g2.setColor(new Color(255,230,120,150));
            g2.fillRoundRect(b.x-1, b.y-3, b.w+2, b.h+6, 6, 6);
            // thân
            g2.setPaint(new GradientPaint(b.x, b.y, new Color(255,255,200),
                    b.x, b.y+b.h, new Color(255,200,0)));
            g2.fillRoundRect(b.x, b.y, b.w, b.h, 6, 6);
        }
    }

    private void drawBalls(Graphics2D g2){
        for (int i=0;i<balls.size();i++){
            Ball ball = balls.get(i);
            // aura
            g2.setColor(new Color(ball.color.getRed(), ball.color.getGreen(), ball.color.getBlue(), 60));
            g2.fillOval(ball.x - ball.r - 7, ball.y - ball.r - 7, ball.r*2+14, ball.r*2+14);

            // thân gradient highlight
            RadialGradientPaint rg = new RadialGradientPaint(
                    new Point2D.Float(ball.x - ball.r*0.3f, ball.y - ball.r*0.35f), ball.r,
                    new float[]{0f, 0.7f, 1f},
                    new Color[]{new Color(255,255,255,220), new Color(ball.color.getRed(),ball.color.getGreen(),ball.color.getBlue(),235), new Color(20,30,60,220)});
            g2.setPaint(rg);
            g2.fillOval(ball.x-ball.r, ball.y-ball.r, ball.r*2, ball.r*2);

            g2.setColor(new Color(255,255,255,120));
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawOval(ball.x-ball.r, ball.y-ball.r, ball.r*2, ball.r*2);
        }
    }

    private void drawPlane(Graphics2D g2){
        int cx = px + pW/2;

        // lửa phản lực dưới đuôi
        int flameH = 18 + rnd.nextInt(8);
        Polygon flame = new Polygon();
        flame.addPoint(cx, py + pH);
        flame.addPoint(cx - 9, py + pH + flameH);
        flame.addPoint(cx + 9, py + pH + flameH);
        g2.setPaint(new GradientPaint(cx, py+pH, new Color(255,240,180,220),
                cx, py+pH+flameH, new Color(255,120,0,160)));
        g2.fillPolygon(flame);

        // vệt khói mờ (đã vẽ ở trails)

        // glow outline
        for (int i=3;i>=1;i--){
            g2.setColor(new Color(120,220,255,28));
            g2.fillRoundRect(px - i, py - i, pW + i*2, pH + i*2, 18+i*2, 18+i*2);
        }

        // thân máy bay (tam giác)
        Polygon body = new Polygon();
        body.addPoint(cx, py);                  // mũi
        body.addPoint(px, py + pH - 10);        // trái
        body.addPoint(px + pW, py + pH - 10);   // phải
        g2.setPaint(new GradientPaint(px, py, new Color(120,200,255),
                                      px, py+pH, new Color(35,60,125)));
        g2.fillPolygon(body);

        // cockpit
        g2.setColor(new Color(255,255,255,160));
        g2.fillRoundRect(cx-10, py+16, 20, 14, 10, 10);

        // cánh
        g2.setColor(new Color(180,220,255));
        g2.fillRoundRect(px-18, py+28, pW+36, 12, 10, 10);

        // đuôi đứng
        g2.setColor(new Color(80,130,200));
        g2.fillRoundRect(cx-7, py+pH-16, 14, 18, 6, 6);

        // viền
        g2.setColor(new Color(255,255,255,140));
        g2.drawPolygon(body);
    }

    private void drawHUD(Graphics2D g2){
        // score (shadow)
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        String s = "Score: " + score;
        g2.setColor(new Color(0,0,0,150)); g2.drawString(s, 18, 30);
        g2.setColor(Color.WHITE); g2.drawString(s, 16, 28);

        // lives (tim)
        int x = W - 170, y = 14;
        for (int i=0;i<lives;i++){
            Shape heart = heartShape(x + i*30, y, 20);
            g2.setColor(new Color(255,85,120,220)); g2.fill(heart);
            g2.setColor(new Color(255,255,255,150)); g2.draw(heart);
        }
    }

    private Shape heartShape(int x,int y,int size){
        int w=size, h=size;
        GeneralPath gp = new GeneralPath();
        gp.moveTo(x + w/2, y + h);
        gp.curveTo(x + w*1.2, y + h*0.65, x + w*0.8, y + h*0.05, x + w/2, y + h*0.35);
        gp.curveTo(x + w*0.2, y + h*0.05, x - w*0.2, y + h*0.65, x + w/2, y + h);
        gp.closePath();
        return gp;
    }

    private void drawCenterBox(Graphics2D g2, String title, String sub){
        // khung mờ
        g2.setColor(new Color(0,0,0,160));
        g2.fillRoundRect(W/2-280, H/2-120, 560, 190, 26, 26);
        // title
        g2.setFont(new Font("Arial", Font.BOLD, 42));
        int tw = g2.getFontMetrics().stringWidth(title);
        g2.setColor(new Color(255,255,255,45)); g2.drawString(title, (W-tw)/2+3, H/2-28+3);
        g2.setColor(Color.WHITE); g2.drawString(title, (W-tw)/2, H/2-28);
        // sub
        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        int sw = g2.getFontMetrics().stringWidth(sub);
        g2.setColor(new Color(225,225,225)); g2.drawString(sub, (W-sw)/2, H/2+18);
    }

    private void drawParticles(Graphics2D g2){
        for (int i=0;i<particles.size();i++){
            Particle p = particles.get(i);
            int a = Math.max(0, (int)(p.life*255));
            g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), a));
            g2.fillRect((int)p.x, (int)p.y, 3, 3);
        }
    }

    private void drawTrails(Graphics2D g2){
        for (int i=0;i<trails.size();i++){
            Trail t = trails.get(i);
            int a = Math.max(0, (int)(t.life*180));
            g2.setColor(new Color(180,220,255, a));
            g2.fillOval((int)(t.x - t.size/2), (int)(t.y - t.size/2), (int)t.size, (int)t.size);
        }
    }

    // ===== Input
    public void keyPressed(KeyEvent e){
        int k = e.getKeyCode();
        if (state == START) {
            if (k == KeyEvent.VK_ENTER) startPlay();
            return;
        }
        if (state == PAUSED) {
            if (k == KeyEvent.VK_P) state = PLAYING;
            else if (k == KeyEvent.VK_ENTER) { resetAll(); startPlay(); }
            return;
        }
        if (state == GAMEOVER) {
            if (k == KeyEvent.VK_ENTER) { resetAll(); startPlay(); }
            return;
        }
        if (state == PLAYING){
            if (k == KeyEvent.VK_LEFT) left = true;
            if (k == KeyEvent.VK_RIGHT) right = true;
            if (k == KeyEvent.VK_SPACE) shoot();
            if (k == KeyEvent.VK_P) state = PAUSED;
        }
    }
    public void keyReleased(KeyEvent e){
        if (e.getKeyCode()==KeyEvent.VK_LEFT) left = false;
        if (e.getKeyCode()==KeyEvent.VK_RIGHT) right = false;
    }
    public void keyTyped(KeyEvent e){}
}