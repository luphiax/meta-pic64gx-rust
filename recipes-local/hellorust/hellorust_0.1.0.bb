DESCRIPTION = "A friendly program that prints Hello World! in RUST" 
PRIORITY = "optional" 
SECTION = "examples" 

LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0-only;md5=801f80980d171dd6425610833a22dbe6"

inherit cargo

SRC_URI = "\
    file://Cargo.toml \
    file://Cargo.lock \
    file://src/main.rs \
"

S = "${WORKDIR}"


do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/target/${CARGO_TARGET_SUBDIR}/hellorust \
        ${D}${bindir}/hellorust
}

