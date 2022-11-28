FROM mcr.microsoft.com/azure-cli

MAINTAINER "HMCTS BSP <https://github.com/hmcts>"
LABEL maintainer = "HMCTS BSP <https://github.com/hmcts>"

COPY init-azurite.sh /

ENTRYPOINT ["/bin/bash", "/init-azurite.sh"]
