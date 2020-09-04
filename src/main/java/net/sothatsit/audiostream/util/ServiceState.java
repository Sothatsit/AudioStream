package net.sothatsit.audiostream.util;

import net.sothatsit.property.DelegatedProperty;
import net.sothatsit.property.Property;

import java.util.Objects;

/**
 * Represents the state of a service. Can be used for any service that can be started and stopped.
 *
 * @author Paddy Lamont
 */
public class ServiceState {

    private final Type type;
    private final String status;
    private final Throwable error;

    public ServiceState(Type type, String status, Throwable error) {
        if (type == null)
            throw new IllegalArgumentException("type cannot be null");
        if (status == null)
            throw new IllegalArgumentException("status cannot be null");

        this.type = type;
        this.status = status;
        this.error = error;
    }

    public String getStateName() {
        return type.toString();
    }

    public String getStatusMessage() {
        return status;
    }

    public boolean hasError() {
        return error != null;
    }

    public Throwable getError() {
        return error;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("ServiceState(");
        builder.append(getStateName());

        if (hasError()) {
            builder.append(", Errored: \"");
            builder.append(getError().getMessage());
            builder.append('"');
        }

        builder.append(")");

        return builder.toString();
    }

    @Override
    public int hashCode() {
        return type.hashCode() ^ Objects.hashCode(error) ^ getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || !getClass().equals(obj.getClass()))
            return false;

        ServiceState other = (ServiceState) obj;
        return type == other.type
                && Objects.equals(status, other.status)
                && Objects.equals(getError(), other.getError());
    }

    /**
     * The type of the state.
     */
    public enum Type {
        STOPPED("Stopped"),
        STOPPING("Stopping"),
        RUNNING("Running"),
        STARTING("Starting");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * A Property that allows updating states more easily.
     */
    public static class StateProperty extends DelegatedProperty<ServiceState> {

        private final Object lock = new Object();

        public StateProperty(String name) {
            super(Property.createNonNull(name, new ServiceState(Type.STOPPED, "Not started", null)));
        }

        @Override
        public void compareAndSet(ServiceState expectedState, ServiceState updatedState) {
            synchronized (lock) {
                super.compareAndSet(expectedState, updatedState);
            }
        }

        @Override
        public void set(ServiceState state) {
            synchronized (lock) {
                super.set(state);
            }
        }

        private Throwable getCarryError(boolean carryError, Throwable newError) {
            if (!carryError || newError != null)
                return newError;

            return get().getError();
        }

        /**
         * Update to a stopped state, carrying through the error from the previous state.
         *
         * @param status A message containing more information about the current state.
         */
        public void setToStopped(String status) {
            setToStopped(status, true, null);
        }

        /**
         * Update to a stopped state.
         *
         * @param status A message containing more information about the current state.
         * @param carryError Whether to carry through the error from the previous state.
         */
        public void setToStopped(String status, boolean carryError) {
            setToStopped(status, carryError, null);
        }

        /**
         * Update to a stopped state.
         *
         * @param status A message containing more information about the current state.
         * @param error An error the service has experienced.
         */
        public void setToStopped(String status, Throwable error) {
            setToStopped(status, true, error);
        }

        /**
         * Update to a stopped state.
         *
         * @param status A message containing more information about the current state.
         * @param carryError Whether to carry through any error from the previous state.
         * @param error An error the service has experienced.
         */
        public void setToStopped(String status, boolean carryError, Throwable error) {
            synchronized (lock) {
                set(new ServiceState(Type.STOPPED, status, getCarryError(carryError, error)));
            }
        }

        /**
         * Update to a stopping state, carrying through the error from the previous state.
         *
         * @param status A message containing more information about the current state.
         */
        public void setToStopping(String status) {
            setToStopping(status, true, null);
        }

        /**
         * Update to a stopping state.
         *
         * @param status A message containing more information about the current state.
         * @param carryError Whether to carry through the error from the previous state.
         */
        public void setToStopping(String status, boolean carryError) {
            setToStopping(status, carryError, null);
        }

        /**
         * Update to a stopping state.
         *
         * @param status A message containing more information about the current state.
         * @param error An error the service has experienced.
         */
        public void setToStopping(String status, Throwable error) {
            setToStopping(status, true, error);
        }

        /**
         * Update to a stopping state.
         *
         * @param status A message containing more information about the current state.
         * @param carryError Whether to carry through any error from the previous state.
         * @param error An error the service has experienced.
         */
        public void setToStopping(String status, boolean carryError, Throwable error) {
            synchronized (lock) {
                set(new ServiceState(Type.STOPPING, status, getCarryError(carryError, error)));
            }
        }

        /**
         * Update to a running state, carrying through the error from the previous state.
         *
         * @param status A message containing more information about the current state.
         */
        public void setToRunning(String status) {
            setToRunning(status, true, null);
        }

        /**
         * Update to a running state.
         *
         * @param status A message containing more information about the current state.
         * @param carryError Whether to carry through the error from the previous state.
         */
        public void setToRunning(String status, boolean carryError) {
            setToRunning(status, carryError, null);
        }

        /**
         * Update to a running state.
         *
         * @param status A message containing more information about the current state.
         * @param error An error the service has experienced.
         */
        public void setToRunning(String status, Throwable error) {
            setToRunning(status, true, error);
        }

        /**
         * Update to a running state.
         *
         * @param status A message containing more information about the current state.
         * @param carryError Whether to carry through any error from the previous state.
         * @param error An error the service has experienced.
         */
        public void setToRunning(String status, boolean carryError, Throwable error) {
            synchronized (lock) {
                set(new ServiceState(Type.RUNNING, status, getCarryError(carryError, error)));
            }
        }

        /**
         * If the current state is errored, set the current state to running with the same status and no error.
         */
        public void clearRunningError() {
            synchronized (lock) {
                ServiceState state = get();
                if (!state.hasError())
                    return;

                setToRunning(state.getStatusMessage(), false);
            }
        }

        /**
         * Update to a starting state, carrying through any error from the previous state.
         *
         * @param status A message containing more information about the current state.
         */
        public void setToStarting(String status) {
            setToStarting(status, true, null);
        }

        /**
         * Update to a starting state.
         *
         * @param status A message containing more information about the current state.
         * @param carryError Whether to carry through any error from the previous state.
         */
        public void setToStarting(String status, boolean carryError) {
            setToStarting(status, carryError, null);
        }

        /**
         * Update to a starting state, carrying through any error from the previous state.
         *
         * @param status A message containing more information about the current state.
         * @param error An error the service has experienced.
         */
        public void setToStarting(String status, Throwable error) {
            setToStarting(status, true, error);
        }

        /**
         * Update to a starting state.
         *
         * @param status A message containing more information about the current state.
         * @param carryError Whether to carry through any error from the previous state.
         * @param error An error the service has experienced.
         */
        public void setToStarting(String status, boolean carryError, Throwable error) {
            synchronized (lock) {
                set(new ServiceState(Type.STARTING, status, getCarryError(carryError, error)));
            }
        }
    }
}
