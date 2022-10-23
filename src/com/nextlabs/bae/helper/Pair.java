package com.nextlabs.bae.helper;

import java.io.Serializable;

public class Pair<K, V> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected K element0;
	protected V element1;

	public static <K, V> Pair<K, V> createPair(K element0, V element1) {
		return new Pair<K, V>(element0, element1);
	}

	public Pair() {
	}

	public Pair(K element0, V element1) {
		this.element0 = element0;
		this.element1 = element1;
	}

	public K getElement0() {
		return element0;
	}

	public V getElement1() {
		return element1;
	}

	public void setElement0(K element0) {
		this.element0 = element0;
	}

	public void setElement1(V element1) {
		this.element1 = element1;
	}

}