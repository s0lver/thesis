package tamps.cinvestav.thesis_v1.model;

import java.util.Date;

/**
 * @author Rafael
 * 
 */
public abstract class Registro {
	private Date timestamp;
	private String tipo;

	public Registro() {
	}

	/**
	 * @param tipo
	 */
	public Registro(String tipo) {
		super();
		this.tipo = tipo;
	}

	public Registro(String tipo, Date fecha) {
		this.tipo = tipo;
		this.timestamp = fecha;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}
}
