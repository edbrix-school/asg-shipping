package com.asg.shipping.portmaster.entity;

import java.io.Serializable;
import java.util.Objects;

public class PortMasterId implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Long groupPoid;
	private Long portPoid;

	public PortMasterId() {
	}

	public PortMasterId(Long groupPoid, Long portPoid) {
		this.groupPoid = groupPoid;
		this.portPoid = portPoid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof PortMasterId))
			return false;
		PortMasterId that = (PortMasterId) o;
		return Objects.equals(groupPoid, that.groupPoid) && Objects.equals(portPoid, that.portPoid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(groupPoid, portPoid);
	}
}