package net.sothatsit.audiostream.view;

import net.sothatsit.audiostream.encryption.EncryptionSettings;
import net.sothatsit.audiostream.model.AudioStreamModel;
import net.sothatsit.property.Property;
import net.sothatsit.property.awt.*;

import javax.swing.*;
import java.awt.*;

/**
 * The panel to use to setup encryption for the connections of AudioStream.
 *
 * @author Paddy Lamont
 */
public class EncryptionPanel extends PropertyPanel {

    public EncryptionPanel(AudioStreamModel model) {
        Property<Boolean> doEncrypt = Property.create("doEncrypt", false);
        Property<String> secret = Property.create("secret", "My secret");

        Property<EncryptionSettings> encryptionSettings = Property.map(
                "encryptionSettings", doEncrypt, secret,
                EncryptionSettings::new
        );
        model.encryptionSettings.set(encryptionSettings);

        setLayout(new GridBagLayout());

        GBCBuilder constraints = new GBCBuilder()
                .anchor(GridBagConstraints.WEST)
                .fill(GridBagConstraints.BOTH)
                .insets(5, 5, 5, 5)
                .weightX(1);

        { // Settings
            add(new PropertySeparator("Settings"), constraints.build(2));
            constraints.nextRow();

            PropertyComboBox<Boolean> doEncryptCombo = new PropertyComboBox<>(
                    new Boolean[] { true, false },
                    doEncrypt,
                    (encrypt) -> (encrypt ? "Yes" : "No")
            );

            add("Encrypt Traffic?", constraints.weightX(0).build());
            add(doEncryptCombo, constraints.build());
            constraints.nextRow();

            Property<String> secretTooltip = doEncrypt.map("secretTooltip", encrypt -> {
                if (encrypt)
                    return "This secret must be shared by both the audio and the server";
                return "Encryption is not enabled";
            });

            PropertyLabel secretLabel = new PropertyLabel("Encryption Secret");
            PropertyTextField secretField = new PropertyTextField(secret);

            secretLabel.setToolTipText(secretTooltip);
            secretLabel.setForeground(Property.ternary("secretLabelFg", doEncrypt, Color.BLACK, Color.DARK_GRAY));
            secretField.setToolTipText(secretTooltip);
            secretField.setEnabled(doEncrypt);

            add(secretLabel, constraints.weightX(0).build());
            add(secretField, constraints.build());
            constraints.nextRow();
        }

        { // Empty Space
            add(new JPanel(), constraints.weight(1.0, 1.0).build(2));
            constraints.nextRow();
        }
    }
}
