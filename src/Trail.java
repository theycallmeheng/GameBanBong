public class Trail {
    public float x,y,vy,life=1f,size=10f;
    public Trail(float x,float y,float dx, float vy,float life){this.x=x+dx;this.y=y;this.vy=vy;this.life=life;}
}