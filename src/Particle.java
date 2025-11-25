import java.awt.Color;

public class Particle {
    public float x,y,vx,vy,life=1f;
    public Color color;
    public Particle(float x,float y,float vx,float vy, Color c, float life){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.color=c;this.life=life;}
}