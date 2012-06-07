/**
 * 
 */
package tamps.cinvestav.thesis_v1.model;

import java.util.Date;

/**
 * @author Rafael Informaci—n relevante para el contexto de coordenadas
 *         geog‡ficas puede ser encontrada en
 *         http://www.manualvuelo.com/NAV/NAV72.html
 */
public class RegistroGPS extends Registro {
	public static final String TIPO = "GPS";

	private double latitud;
	private double longitud;
	private float precision;

	/**
	 * 
	 */
	public RegistroGPS() {
		super("GPS");
	}

	/**
	 * @param fecha
	 */
	public RegistroGPS(Date fecha) {
		super("GPS", fecha);
	}

	/**
	 * @param fecha
	 * @param latitud
	 * @param longitud
	 */
	public RegistroGPS(Date fecha, double latitud, double longitud,
			float precision) {
		super("GPS", fecha);
		this.latitud = latitud;
		this.longitud = longitud;
		this.precision = precision;
	}

	public double getLatitud() {
		return latitud;
	}

	public void setLatitud(double latitud) {
		this.latitud = latitud;
	}

	public double getLongitud() {
		return longitud;
	}

	public void setLongitud(double longitud) {
		this.longitud = longitud;
	}

	public float getPrecision() {
		return precision;
	}

	public void setPrecision(float precision) {
		this.precision = precision;
	}

	@Override
	public String toString() {
		return new StringBuffer().append("Reg. GPS: Lat=").append(latitud)
				.append(" Long=").append(longitud).append(" Prec=")
				.append(precision).append(" timestamp=").append(getTimestamp())
				.toString();
	}
}
