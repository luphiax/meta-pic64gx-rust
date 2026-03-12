SUMMARY = "PIC64GX standalone Zephyr application loaded by HSS"
DESCRIPTION = "Builds a selectable standalone Zephyr application for PIC64GX and deploys it for HSS payload generation"

require recipes-kernel/zephyr-kernel/zephyr-sample.inc

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "pic64gx-curiosity-kit-amp"

PIC64GX_ZEPHYR_SUPPORTED_APPS ?= "blinky_amp"
PIC64GX_ZEPHYR_EXAMPLES_SRCREV ?= "e695942408e44e4ccccc5b8823c7c17e14267681"

ZEPHYR_BRANCH = "main"
SRCREV_default = "d5777557c54cec9cd4b3db66e3ff413987eee393"
SRCREV_pic64-zephyr-examples = "${PIC64GX_ZEPHYR_EXAMPLES_SRCREV}"

SRC_URI_ZEPHYR = "git://github.com/pic64gx/pic64gx-zephyr.git;protocol=https"
SRC_URI_APP = "git://github.com/pic64gx/pic64gx-zephyr-examples.git;protocol=https;subpath=apps/${PIC64GX_ZEPHYR_APP}"
SRC_URI:append = " ${SRC_URI_ZEPHYR};nobranch=1;name=default;destsuffix=git/zephyr"
SRC_URI:append = " ${SRC_URI_APP};name=pic64-zephyr-examples;nobranch=1;destsuffix=git/pic64gx-soc/apps/${PIC64GX_ZEPHYR_APP}"

ZEPHYR_SRC_DIR = "${WORKDIR}/git/pic64gx-soc/apps/${PIC64GX_ZEPHYR_APP}"

python () {
    import re

    app = d.getVar("PIC64GX_ZEPHYR_APP") or ""
    supported = sorted(filter(None, (d.getVar("PIC64GX_ZEPHYR_SUPPORTED_APPS") or "").split()))

    if not re.match(r"^[A-Za-z0-9_-]+$", app):
        bb.fatal(f"{d.getVar('PN')}: invalid PIC64GX_ZEPHYR_APP '{app}'")

    if app not in supported:
        bb.fatal(
            f"{d.getVar('PN')}: unsupported PIC64GX_ZEPHYR_APP '{app}'. "
            f"Supported values: {' '.join(supported)}"
        )
}

do_deploy:append() {
    install -m 0644 ${B}/zephyr/${ZEPHYR_MAKE_OUTPUT} ${DEPLOYDIR}/pic64gx-zephyr-${PIC64GX_ZEPHYR_APP}.elf
    install -m 0644 ${B}/zephyr/${ZEPHYR_MAKE_OUTPUT} ${DEPLOYDIR}/zephyr-amp-application.elf
}
