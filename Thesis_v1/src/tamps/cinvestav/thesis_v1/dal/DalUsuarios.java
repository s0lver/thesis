package tamps.cinvestav.thesis_v1.dal;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.json.JSONException;

import tamps.cinvestav.thesis_v1.model.Usuario;
import tamps.cinvestav.thesis_v1.nal.ClientHTTPRequest;
import tamps.cinvestav.thesis_v1.tools.Utilerias;
import android.util.Log;

/**
 * @author Rafael
 * 
 */
public class DalUsuarios {
	private static ClientHTTPRequest conectorRemoto;
	private static SimpleDateFormat formatoFecha = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm");

	/***
	 * 
	 * @param usuario
	 * @return
	 * @throws IOException
	 */
	public int agregarUsuario(Usuario usuario) throws IOException {
		conectorRemoto = new ClientHTTPRequest(Utilerias.STRING_URL_SERVICIO
				+ "agregarUsuario.php");

		conectorRemoto.setParameter("agregar_usuario", usuario.getNombre());
		conectorRemoto.setParameter("nombre", usuario.getNombre());
		conectorRemoto.setParameter("username", usuario.getUsername());
		conectorRemoto.setParameter("password", usuario.getPassword());

		String fechaIngreso = formatoFecha.format(usuario.getFechaIngreso());
		Log.i("Cinvestav DalUsuarios agregarUsuario", "Fecha generada: "
				+ fechaIngreso);
		conectorRemoto.setParameter("fecha_ingreso", fechaIngreso);

		String respuesta = Utilerias.obtenerStringRespuesta(conectorRemoto
				.post());

		Log.i("Cinvestav DalUsuarios agregarUsuario",
				"Respuesta desde el server: " + respuesta);
		if (Integer.parseInt(respuesta.trim()) == (Utilerias.INT_USUARIO_EXISTE)) {
			Log.w("Cinvestav DalUsuarios agregarUsuario",
					"Usuario existe, retornando "
							+ Utilerias.INT_USUARIO_EXISTE);
		} else {
			Log.i("Cinvestav DalUsuarios agregarUsuario",
					"Usuario " + usuario.getNombre() + " ha sido agregado");
		}

		return Integer.parseInt(respuesta.trim());
	}

	/***
	 * Obtiene los datos de un usuario proporcionando un id
	 * 
	 * @param id
	 *            El Id del usuario a buscar.
	 * @return El objeto usuario correspondiente al username especificado, null
	 *         en caso de no encontrar resultados
	 * @throws IOException
	 * @throws JSONException
	 * @throws ParseException
	 */
	public Usuario obtenerUsuario(int id) throws IOException, JSONException,
			ParseException {
		conectorRemoto = new ClientHTTPRequest(Utilerias.STRING_URL_SERVICIO
				+ "obtenerUsuario.php");
		conectorRemoto.setParameter("busqueda_por_id", id);
		conectorRemoto.setParameter("id_usuario", id);

		String respuesta = Utilerias.obtenerStringRespuesta(conectorRemoto
				.post());
		if (respuesta.equals(Utilerias.STRING_BUSQUEDA_SIN_RESULTADOS))
			return null;
		else
			return Utilerias.crearUsuarioDesdeStringJson(respuesta);
	}

	/***
	 * Obtiene los datos de un usuario proporcionando un "username"
	 * 
	 * @param usuario
	 *            El nombre del usuario (username) a buscar en el sistema
	 * @return El objeto usuario correspondiente al username especificado
	 * @throws IOException
	 * @throws JSONException
	 * @throws ParseException
	 */
	public Usuario obtenerUsuario(String usuario) throws IOException,
			JSONException, ParseException {
		conectorRemoto = new ClientHTTPRequest(Utilerias.STRING_URL_SERVICIO
				+ "obtenerUsuario.php");
		conectorRemoto.setParameter("busqueda_por_usuario", usuario);
		conectorRemoto.setParameter("usuario", usuario);

		String respuesta = Utilerias.obtenerStringRespuesta(conectorRemoto
				.post());
		if (respuesta.equals(Utilerias.STRING_BUSQUEDA_SIN_RESULTADOS))
			return null;
		else
			return Utilerias.crearUsuarioDesdeStringJson(respuesta);
	}

	/***
	 * Obtiene los datos de un usuario proporcionando el nombre de usuario
	 * ("username") y la contrase–a. Funciona a modo de "login"
	 * 
	 * @param usuario
	 *            El nombre de usuario a buscar
	 * @param contrasenia
	 *            La contrasenia a buscar
	 * @return El objeto Usuario asociado al username y password especificados.
	 *         Null si no existe un usuario con dichos atributos.
	 * @throws IOException
	 * @throws JSONException
	 * @throws ParseException
	 */
	public Usuario obtenerUsuarioPorUsernameYContrasenia(String username,
			String password) throws IOException, JSONException, ParseException {
		conectorRemoto = new ClientHTTPRequest(Utilerias.STRING_URL_SERVICIO
				+ "login.php");
		conectorRemoto.setParameter("login", username);
		conectorRemoto.setParameter("username", username);
		conectorRemoto.setParameter("password", password);

		String respuesta = Utilerias.obtenerStringRespuesta(conectorRemoto
				.post());

		if (respuesta.equals(Utilerias.STRING_BUSQUEDA_SIN_RESULTADOS)) {
			Log.e("Cinvestav DalUsuarios OUPUserYCont",
					"respuesta nula desde el servidor");
			return null;
		} else {
			return Utilerias.crearUsuarioDesdeStringJson(respuesta);
		}
	}
}
