package ch.planetebowl.printagent.ui.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/** Champ de reglage standard : grand, avec message d'erreur sous le champ — pense pour
 * etre lu/rempli au comptoir, pas sur un ecran de bureau. */
@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        isError = errorMessage != null,
        supportingText = {
            Text(text = errorMessage ?: supportingText.orEmpty(), style = MaterialTheme.typography.bodyMedium)
        },
        // isPassword force KeyboardType.Password (pas seulement le masquage visuel) : c'est
        // ce type de clavier qui desactive reellement la correction/capitalisation
        // automatique cote IME. Sans ca, un champ masque par des points laisse l'utilisateur
        // taper "a l'aveugle" pendant qu'un clavier texte standard autocorrige silencieusement
        // le contenu (ex: un token) sans qu'il puisse s'en apercevoir a l'ecran.
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
            autoCorrectEnabled = !isPassword,
        ),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = trailingIcon,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = TextFieldDefaults.colors(),
        singleLine = true,
    )
}
