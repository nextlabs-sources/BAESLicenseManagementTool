package com.nextlabs.bae.helper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SearchInput<K, V> extends Pair<K, V> implements Serializable {

	private static final long serialVersionUID = 1L;
	private List<String> inputs;
	private boolean operator;
	private boolean isProject;

	public SearchInput(K element0, V element1) {
		super(element0, element1);
		inputs = new ArrayList<String>();
		operator = true;
		if (element0.equals(PropertyLoader.bAESProperties.getProperty("pa"))) {
			isProject = true;
		} else {
			isProject = false;
		}
	}

	public List<String> getInputs() {
		return inputs;
	}

	public void setInputs(List<String> inputs) {
		this.inputs = inputs;
	}

	public boolean isOperator() {
		return operator;
	}

	public void setOperator(boolean operator) {
		this.operator = operator;
	}

	public boolean getIsProject() {
		return isProject;
	}

	public void setIsProject(boolean isProject) {
		this.isProject = isProject;
	}

}
