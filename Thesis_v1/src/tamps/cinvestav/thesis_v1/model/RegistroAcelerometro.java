/**
 * 
 */
package tamps.cinvestav.thesis_v1.model;

import java.util.Date;

/**
 * @author Rafael
 * 
 */
public class RegistroAcelerometro extends Registro {
	public static final String TIPO = "acc";

	private float ejeX;
	private float ejeY;
	private float ejeZ;

	/**
	 * 
	 */
	public RegistroAcelerometro() {
		super("acelerometro");
	}

	/**
	 * @param tipo
	 * @param fecha
	 */
	public RegistroAcelerometro(Date fecha) {
		super("acelerometro", fecha);
	}

	/**
	 * @param fecha
	 * @param ejeX
	 * @param ejeY
	 * @param ejeZ
	 */
	public RegistroAcelerometro(Date fecha, float ejeX, float ejeY, float ejeZ) {
		super("acelerometro", fecha);
		this.ejeX = ejeX;
		this.ejeY = ejeY;
		this.ejeZ = ejeZ;
	}

	public float getEjeX() {
		return ejeX;
	}

	public void setEjeX(float ejeX) {
		this.ejeX = ejeX;
	}

	public float getEjeY() {
		return ejeY;
	}

	public void setEjeY(float ejeY) {
		this.ejeY = ejeY;
	}

	public float getEjeZ() {
		return ejeZ;
	}

	public void setEjeZ(float ejeZ) {
		this.ejeZ = ejeZ;
	}

	@Override
	public String toString() {
		return new StringBuffer().append("Reg. acelerometro: X=").append(ejeX)
				.append(" Y=").append(ejeY).append(" Z=").append(ejeZ)
				.toString();
	}
}
