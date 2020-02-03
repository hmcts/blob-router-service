package uk.gov.hmcts.reform.blobrouter.config;

import javax.validation.constraints.NotNull;

public class StorageConfigItem {

    private int sasValidity;

    @NotNull
    private TargetStorageAccount targetStorageAccount;

    @NotNull
    private String sourceContainer;

    @NotNull
    private String targetContainer;

    private boolean isEnabled = true;

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

    public String getSourceContainer() {
        return sourceContainer;
    }

    public void setSourceContainer(String sourceContainer) {
        this.sourceContainer = sourceContainer;
    }

    public String getTargetContainer() {
        return targetContainer;
    }

    public void setTargetContainer(String targetContainer) {
        this.targetContainer = targetContainer;
    }
}
