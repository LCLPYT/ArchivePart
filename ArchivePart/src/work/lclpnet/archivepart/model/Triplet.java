package work.lclpnet.archivepart.model;

public class Triplet<A, B, C> {

	public A a;
	public B b;
	public C c;
	
	public Triplet(A a, B b, C c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}
	
	public static <A, B, C> Triplet<A, B, C> of(A a, B b, C c) {
		return new Triplet<A, B, C>(a, b, c);
	}
	
}
