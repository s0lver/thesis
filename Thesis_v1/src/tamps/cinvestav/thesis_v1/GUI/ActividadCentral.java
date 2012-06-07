package tamps.cinvestav.thesis_v1.GUI;

import tamps.cinvestav.thesis_v1.R;
import tamps.cinvestav.thesis_v1.model.Usuario;
import tamps.cinvestav.thesis_v1.services.Servicio;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ActividadCentral extends Activity {
	static Intent servicio = null;
	Usuario usuarioActivo = ActividadLogin.USUARIO_ACTIVO;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.central);

		TextView lblBienvenida = (TextView) findViewById(R.id.lblMensajeBienvenida_AC);
		lblBienvenida.setText("Bienvenid@ " + usuarioActivo.getUsername());

		if (servicio == null)
			servicio = new Intent(this, Servicio.class);

	}

	public void click(View v) {
		// Click sobre las etiquetas - TextView's
		if (v instanceof TextView) {
			TextView tv = (TextView) (v);
			if (tv == findViewById(R.id.lblRegresar_AC)) {
				finish();
			} else if (tv == findViewById(R.id.lblSensarAcelerometro_AC)) {

			}
			// } else if (tv == findViewById(R.id.lblSensarAcelerometro_AC)) {
			// if (!MiServicio.sensarAcelerometro) {
			// // MiServicio.servicioActivo = true;
			//
			//
			//
			// ((TextView) findViewById(R.id.lblSensarAcelerometro_AC))
			// .setText(R.string.strlblActDetenerAcelerometro_AC);
			// } else {
			// // MiServicio.servicioActivo = false;
			// .
			// ((TextView) findViewById(R.id.lblSensarAcelerometro_AC))
			// .setText(R.string.strlblActAcelerometro_AC);
			// }
			// }
		}
	}
}
