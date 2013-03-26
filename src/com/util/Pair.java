package com.util;

public class Pair<T1, T2> {
	  public T1 o1;
	  public T2 o2;
	  public Pair(T1 o1, T2 o2) { this.o1 = o1; this.o2 = o2; }


	  public static boolean same(Object o1, Object o2) {
	    return o1 == null ? o2 == null : o1.equals(o2);
	  }
	 
	  public T1 getFirst() { return o1; }
	  public T2 getSecond() { return o2; }
	 
	  void setFirst(T1 o) { o1 = o; }
	  void setSecond(T2 o) { o2 = o; }
	 
	  public boolean equals(Object obj) {
	    if( ! (obj instanceof Pair))
	      return false;
	    Pair p = (Pair)obj;
	    return same(p.o1, this.o1) && same(p.o2, this.o2);
	  }
	 
	  public String toString() {
	    return "Pair{"+o1+", "+o2+"}";
	  }
	 
}
