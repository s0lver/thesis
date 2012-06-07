package tamps.cinvestav.thesis_v1.model;

import java.util.Date;

public class Usuario {
	private int id;
	private String nombre;
	private String username;
	private String password;
	private Date fechaIngreso;

	public Usuario() {

	}

	public Usuario(int id, String nombre, String username, String password,
			Date fechaIngreso) {
		super();
		this.id = id;
		this.nombre = nombre;
		this.username = username;
		this.password = password;
		this.fechaIngreso = fechaIngreso;
	}

	public Usuario(String nombre, String username, String password,
			Date fechaIngreso) {
		super();
		this.nombre = nombre;
		this.username = username;
		this.password = password;
		this.fechaIngreso = fechaIngreso;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String login) {
		this.username = login;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Date getFechaIngreso() {
		return fechaIngreso;
	}

	public void setFechaIngreso(Date fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}

	public String toString() {
		return "Id: " + id + "\nNombre: " + nombre + "\nUsername " + username
				+ "\nPassword " + password + "\nFecha de ingreso "
				+ fechaIngreso;

	}
}
