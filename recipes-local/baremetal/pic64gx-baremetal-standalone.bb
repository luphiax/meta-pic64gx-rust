SUMMARY = "PIC64GX standalone baremetal firmware loaded by HSS"
DESCRIPTION = "Builds a selectable baremetal Rust app for PIC64GX and deploys it for HSS payload generation"

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "pic64gx-curiosity-kit-amp"
PIC64GX_BAREMETAL_SUPPORTED_APPS ?= "test2_init_uart"
PIC64GX_BAREMETAL_REPO ?= "git://github.com/luphiax/pic64gx;protocol=https"
PIC64GX_BAREMETAL_SRCREV ?= "2ed34d73bb28b262e0aaf1f3e87f669d5e04f430"
PIC64GX_BAREMETAL_TARGET ?= "riscv64gc-unknown-none-elf"

inherit deploy

HOSTTOOLS += "cargo rustc"

SRCREV_pic64-baremetal = "${PIC64GX_BAREMETAL_SRCREV}"

SRC_URI = "${PIC64GX_BAREMETAL_REPO};name=pic64-baremetal;nobranch=1;destsuffix=git/pic64gx"
S = "${WORKDIR}/git/pic64gx"

do_compile[network] = "1"
do_configure[noexec] = "1"
do_install[noexec] = "1"

python () {
    import re

    app = d.getVar("PIC64GX_BAREMETAL_APP") or ""
    supported = sorted(filter(None, (d.getVar("PIC64GX_BAREMETAL_SUPPORTED_APPS") or "").split()))

    if not re.match(r"^[A-Za-z0-9_-]+$", app):
        bb.fatal(f"{d.getVar('PN')}: invalid PIC64GX_BAREMETAL_APP '{app}'")

    if app not in supported:
        bb.fatal(
            f"{d.getVar('PN')}: unsupported PIC64GX_BAREMETAL_APP '{app}'. "
            f"Supported values: {' '.join(supported)}"
        )
}

do_compile() {
    app_src="${S}/examples/${PIC64GX_BAREMETAL_APP}.rs"

    if [ ! -f ${app_src} ]; then
        bbfatal "Missing ${app_src} from ${PIC64GX_BAREMETAL_REPO}."
    fi

    if [ ! -f ${S}/Cargo.toml ]; then
        bbfatal "Missing fetched baremetal crate ${S}/Cargo.toml from ${PIC64GX_BAREMETAL_REPO}."
    fi

    export PATH="/usr/bin:/bin:${HOME}/.cargo/bin:${PATH}"

    export CARGO_HOME="${CARGO_HOME:-${HOME}/.cargo}"
    export RUSTUP_HOME="${RUSTUP_HOME:-${HOME}/.rustup}"
    export CARGO_TARGET_DIR="${B}/target"
    export CARGO_TARGET_X86_64_UNKNOWN_LINUX_GNU_LINKER="gcc"
    export CC="gcc"

    cd ${S}
    cargo_lock_arg=""
    if [ -f ${S}/Cargo.lock ]; then
        cargo_lock_arg="--locked"
    fi

    cargo build \
        ${cargo_lock_arg} \
        --release \
        --target ${PIC64GX_BAREMETAL_TARGET} \
        --features rt \
        --example ${PIC64GX_BAREMETAL_APP}
}

do_deploy() {
    firmware="${B}/target/${PIC64GX_BAREMETAL_TARGET}/release/examples/${PIC64GX_BAREMETAL_APP}"

    if [ ! -f ${firmware} ]; then
        bbfatal "Missing built baremetal ELF ${firmware}"
    fi

    install -m 0644 ${firmware} ${DEPLOYDIR}/pic64gx-baremetal-${PIC64GX_BAREMETAL_APP}.elf
}

addtask deploy after do_compile before do_build
