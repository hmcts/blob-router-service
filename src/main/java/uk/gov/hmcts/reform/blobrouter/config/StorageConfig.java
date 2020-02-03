package uk.gov.hmcts.reform.blobrouter.config;

import javax.validation.constraints.NotNull;

public class StorageConfig {
    private String name;
    private int sasValidity;

    @NotNull
    private TargetStorageAccount targetStorageAccount;

    @NotNull
    private String targetContainer;

    private boolean isEnabled = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSasValidity() {
        return sasValidity;
    }

    public void setSasValidity(int sasValidity) {
        this.sasValidity = sasValidity;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public TargetStorageAccount getTargetStorageAccount() {
        return targetStorageAccount;
    }

    public void setTargetStorageAccount(TargetStorageAccount targetStorageAccount) {
        this.targetStorageAccount = targetStorageAccount;
    }

    public String getTargetContainer() {
        return targetContainer;
    }

    public void setTargetContainer(String targetContainer) {
        this.targetContainer = targetContainer;
    }
}
