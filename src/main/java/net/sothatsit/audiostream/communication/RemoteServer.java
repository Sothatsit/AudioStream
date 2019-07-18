package net.sothatsit.audiostream.communication;

import net.sothatsit.audiostream.model.RemoteServerDetails;
import net.sothatsit.property.Property;

import java.net.*;

/**
 * A class used to represent a remote audio server that can be connected to.
 *
 * @author Paddy Lamont
 */
public class RemoteServer {

    private final InetSocketAddress controlAddress;
    private final Property<RemoteServerDetails> details;
    private final Property<Long> lastUpdate;

    public RemoteServer(InetSocketAddress controlAddress) {
        this.controlAddress = controlAddress;
        this.details = Property.create("details");
        this.lastUpdate = Property.createNonNull("lastUpdate", System.currentTimeMillis());
    }

    public InetSocketAddress getControlAddress() {
        return controlAddress;
    }

    public String getAddressString() {
        String addressString;
        if (isAddressLocalhost(controlAddress)) {
            addressString = "localhost";
        } else {
            addressString = controlAddress.getHostName();
            if (addressString == null) {
                addressString = controlAddress.getAddress().getHostAddress();
            }
        }

        return addressString + ":" + controlAddress.getPort();
    }

    public Property<RemoteServerDetails> getDetails() {
        return details.readOnly();
    }

    public long getTimeSinceUpdateMS() {
        return System.currentTimeMillis() - lastUpdate.get();
    }

    public void updateDetails(RemoteServerDetails details) {
        this.details.set(details);
        this.lastUpdate.set(System.currentTimeMillis());
    }

    public boolean is(InetSocketAddress address) {
        if (this.controlAddress.equals(address))
            return true;
        return isAddressLocalhost(this.controlAddress) && isAddressLocalhost(address);
    }

    public boolean is(InetAddress address, int port) {
        return is(new InetSocketAddress(address, port));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().equals(obj.getClass()))
            return false;

        RemoteServer other = (RemoteServer) obj;
        return is(other.controlAddress);
    }

    @Override
    public int hashCode() {
        return controlAddress.hashCode();
    }

    @Override
    public String toString() {
        return "RemoteServer(" + controlAddress + ")";
    }

    private static boolean isAddressLocalhost(InetSocketAddress address) {
        return isAddressLocalhost(address.getAddress());
    }

    private static boolean isAddressLocalhost(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress())
            return true;

        try {
            return NetworkInterface.getByInetAddress(address) != null;
        } catch (SocketException e) {
            return false;
        }
    }
}
