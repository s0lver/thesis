package tamps.cinvestav.thesis_v1.GUI;

import java.io.IOException;

import tamps.cinvestav.thesis_v1.R;
import tamps.cinvestav.thesis_v1.model.Usuario;
import tamps.cinvestav.thesis_v1.services.Servicio;
import tamps.cinvestav.thesis_v1.tools.Utilerias;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ActividadRegistrarUsuario extends Activity {

	private Servicio instanciaServicio;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_registrar);
		instanciaServicio = ActividadLogin.instanciaServicio;
	}

	public void click(View v) {
		// Click al boton de regresar a la actividad de Login
		if ((Button) (v) == (Button) findViewById(R.id.btnRegresar_AR)) {
			finish();
		}

		// Click al boton de registrar usuario
		if ((Button) (v) == (Button) findViewById(R.id.btnRegistrarUsuario_AR)) {

			String nombreUsuario = ((EditText) (findViewById(R.id.txtNombreUsuario_AR)))
					.getText().toString();
			String username = ((EditText) (findViewById(R.id.txtUsername_AR)))
					.getText().toString();
			String password = ((EditText) (findViewById(R.id.txtPassword_AR)))
					.getText().toString();
			String passwordConfirmado = ((EditText) (findViewById(R.id.txtConfirmarPassword_AR)))
					.getText().toString();

			// 1. Verificar que ambos passwords coincidan
			if (!password.equals(passwordConfirmado)) {
				Toast.makeText(getApplicationContext(),
						"Passwords no coinciden!", Toast.LENGTH_LONG).show();
				Log.w("Cinvestav ActividadRegistrarUsuario",
						"Passwords no coinciden!");
				return;
			}

			// 2. Intentar agregar el usuario al sistema
			try {
				Usuario usuario = instanciaServicio.registrarUsuario(
						nombreUsuario, username, password);
				if (usuario.getId() == Utilerias.INT_USUARIO_EXISTE) {
					Toast.makeText(
							getApplicationContext(),
							"El dato username ya se encuentra registrado en el sistema.\nEspecifique uno distinto",
							Toast.LENGTH_LONG).show();
					return;
				}
				Toast.makeText(getApplicationContext(),
						"Usuario insertado:\n" + usuario.toString(),
						Toast.LENGTH_LONG).show();
			} catch (IOException e) {
				Log.e("Cinvestav ActividadRegistrarUsuario", e.getMessage());
			}
		}

	}
}
