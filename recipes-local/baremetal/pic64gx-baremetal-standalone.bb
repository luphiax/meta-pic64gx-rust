SUMMARY = "PIC64GX standalone baremetal firmware loaded by HSS"
DESCRIPTION = "Builds a selectable baremetal Rust app for PIC64GX and deploys it for HSS payload generation"

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "pic64gx-curiosity-kit-amp"
PIC64GX_BAREMETAL_SUPPORTED_APPS ?= "test2_init_uart"
PIC64GX_BAREMETAL_USE_LOCAL_BASE = "${@'1' if (d.getVar('PIC64GX_BAREMETAL_SRC') or '').strip() else '0'}"

inherit deploy

HOSTTOOLS += "cargo rustc"

SRCREV_pic64baremetalbase = "${PIC64GX_BAREMETAL_BASE_SRCREV}"
SRCREV_pic64baremetalapp = "${PIC64GX_BAREMETAL_EXAMPLES_SRCREV}"
SRCREV_FORMAT = "pic64baremetalbase_pic64baremetalapp"

SRC_URI_BASE = "${@'' if d.getVar('PIC64GX_BAREMETAL_USE_LOCAL_BASE') == '1' else d.getVar('PIC64GX_BAREMETAL_BASE_REPO') + ';type=git-dependency;name=pic64baremetalbase;nobranch=1;destsuffix=git/pic64gx'}"
SRC_URI_APP = "${PIC64GX_BAREMETAL_EXAMPLES_REPO};subpath=apps/${PIC64GX_BAREMETAL_EXAMPLE};type=git-dependency;name=pic64baremetalapp;nobranch=1;destsuffix=git/pic64gx-baremetal-apps/apps/${PIC64GX_BAREMETAL_EXAMPLE}"
SRC_URI = "${SRC_URI_APP}"
SRC_URI:prepend = "${SRC_URI_BASE} "

do_compile[network] = "1"
do_configure[noexec] = "1"
do_install[noexec] = "1"

python () {
    import os
    import re

    example = d.getVar("PIC64GX_BAREMETAL_EXAMPLE") or ""
    local_base = (d.getVar("PIC64GX_BAREMETAL_SRC") or "").strip()
    supported = sorted(filter(None, (d.getVar("PIC64GX_BAREMETAL_SUPPORTED_APPS") or "").split()))

    if not re.match(r"^[A-Za-z0-9_-]+$", example):
        bb.fatal(f"{d.getVar('PN')}: invalid PIC64GX_BAREMETAL_EXAMPLE '{example}'")

    if example not in supported:
        bb.fatal(
            f"{d.getVar('PN')}: unsupported PIC64GX_BAREMETAL_EXAMPLE '{example}'. "
            f"Supported values: {' '.join(supported)}"
        )

    if local_base and not os.path.isfile(os.path.join(local_base, "Cargo.toml")):
        bb.fatal(
            f"{d.getVar('PN')}: PIC64GX_BAREMETAL_SRC='{local_base}' does not point to a baremetal crate root"
        )
}

do_compile() {
    app_root="${WORKDIR}/git/pic64gx-baremetal-apps/apps/${PIC64GX_BAREMETAL_EXAMPLE}"
    app_src="${app_root}/src/main.rs"
    base_root="${PIC64GX_BAREMETAL_SRC}"
    stage_root="${WORKDIR}/cargo-stage"
    stage="${stage_root}/pic64gx-stage"

    if [ ! -f ${app_src} ]; then
        bbfatal "Missing ${app_src} from ${PIC64GX_BAREMETAL_EXAMPLES_REPO}."
    fi

    if [ -z "${base_root}" ]; then
        base_root="${WORKDIR}/git/pic64gx"
    fi

    if [ ! -f ${base_root}/Cargo.toml ]; then
        if [ -n "${PIC64GX_BAREMETAL_SRC}" ]; then
            bbfatal "Missing ${base_root}/Cargo.toml. Set PIC64GX_BAREMETAL_SRC to the baremetal crate root."
        fi

        bbfatal "Missing fetched base crate ${base_root}/Cargo.toml from ${PIC64GX_BAREMETAL_BASE_REPO}."
    fi

    export PATH="/usr/bin:/bin:${HOME}/.cargo/bin:${PATH}"

    export CARGO_HOME="${CARGO_HOME:-${HOME}/.cargo}"
    export RUSTUP_HOME="${RUSTUP_HOME:-${HOME}/.rustup}"
    export CARGO_TARGET_DIR="${B}/target"
    export CARGO_TARGET_X86_64_UNKNOWN_LINUX_GNU_LINKER="gcc"
    export CC="gcc"

    rm -rf ${stage_root}
    mkdir -p ${stage}/examples

    cp ${base_root}/Cargo.toml ${stage}/Cargo.toml
    if [ -f ${base_root}/Cargo.lock ]; then
        cp ${base_root}/Cargo.lock ${stage}/Cargo.lock
    fi
    cp ${base_root}/build.rs ${stage}/build.rs
    cp ${base_root}/device.x ${stage}/device.x
    cp ${base_root}/link.x ${stage}/link.x
    cp ${base_root}/memory.x ${stage}/memory.x
    cp ${base_root}/rust-toolchain.toml ${stage}/rust-toolchain.toml
    cp -a ${base_root}/.cargo ${stage}/.cargo
    cp -a ${base_root}/src ${stage}/src
    cp ${app_src} ${stage}/examples/${PIC64GX_BAREMETAL_EXAMPLE}.rs

    cd ${stage}
    cargo_lock_arg=""
    if [ -f ${stage}/Cargo.lock ]; then
        cargo_lock_arg="--locked"
    fi

    cargo build \
        ${cargo_lock_arg} \
        --release \
        --target ${PIC64GX_BAREMETAL_TARGET} \
        --features rt \
        --example ${PIC64GX_BAREMETAL_EXAMPLE}
}

do_deploy() {
    firmware="${B}/target/${PIC64GX_BAREMETAL_TARGET}/release/examples/${PIC64GX_BAREMETAL_EXAMPLE}"

    if [ ! -f ${firmware} ]; then
        bbfatal "Missing built baremetal ELF ${firmware}"
    fi

    install -m 0644 ${firmware} ${DEPLOYDIR}/pic64gx-baremetal-${PIC64GX_BAREMETAL_EXAMPLE}.elf
}

addtask deploy after do_compile before do_build
