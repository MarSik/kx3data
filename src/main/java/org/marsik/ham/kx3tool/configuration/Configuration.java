package org.marsik.ham.kx3tool.configuration;

import javax.inject.Singleton;

@Singleton
public class Configuration {
    public Macro getMacro(int id) {
        return new Macro("Macro " + id, "");
    }

    public void setMacro(int td, Macro macro) {

    }
}
