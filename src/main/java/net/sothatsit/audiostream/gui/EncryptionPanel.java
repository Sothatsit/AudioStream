package net.sothatsit.audiostream.gui;

import net.sothatsit.audiostream.encryption.Encryption;
import net.sothatsit.audiostream.encryption.EncryptionSettings;
import net.sothatsit.property.Property;
import net.sothatsit.property.gui.*;

import javax.swing.*;
import java.awt.*;

/**
 * The panel to use to setup encryption for the connections of AudioStream.
 *
 * @author Paddy Lamont
 */
public class EncryptionPanel extends PropertyPanel {

    private final Property<Boolean> doEncrypt;
    private final Property<String> secret;
    private final Property<EncryptionSettings> encryptionSettings;
    private final Property<Encryption> encryption;

    public EncryptionPanel() {
        this.doEncrypt = Property.create("doEncrypt", false);
        this.secret = Property.create("secret", "My secret");

        this.encryptionSettings = Property.map("encryptionSettings", doEncrypt, secret, EncryptionSettings::new);
        this.encryption = encryptionSettings.map("encryption", EncryptionSettings::createEncryption);

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
                    return "This secret must be shared by both the client and the server";
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

    public Property<EncryptionSettings> getEncryptionSettings() {
        return encryptionSettings;
    }

    public Property<Encryption> getEncryption() {
        return encryption.readOnly();
    }
}
