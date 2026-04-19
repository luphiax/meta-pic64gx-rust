FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append:pic64gx-curiosity-kit-amp = " file://amp-rust.yaml.in"

python __anonymous() {
    if d.getVar("MACHINE") != "pic64gx-curiosity-kit-amp":
        return

    provider = (d.getVar("PIC64GX_STANDALONE_FIRMWARE_PROVIDER") or "zephyr").strip()

    if provider == "zephyr":
        recipe = "pic64gx-zephyr-standalone"
        label = d.getVar("PIC64GX_ZEPHYR_APP") or "blinky_amp"
    elif provider == "baremetal":
        example = d.getVar("PIC64GX_BAREMETAL_EXAMPLE") or "test2_init_uart"
        recipe = "pic64gx-baremetal-standalone"
        label = "baremetal-%s" % example
    else:
        bb.fatal(
            "%s: unsupported PIC64GX_STANDALONE_FIRMWARE_PROVIDER '%s'. "
            "Supported values: baremetal zephyr"
            % (d.getVar("FILE"), provider)
        )

    d.setVar("PIC64GX_STANDALONE_FIRMWARE_RECIPE", recipe)
    d.setVar("PIC64GX_STANDALONE_FIRMWARE_LABEL", label)
}

do_deploy[depends] += "${@' %s:do_deploy' % d.getVar('PIC64GX_STANDALONE_FIRMWARE_RECIPE') if d.getVar('MACHINE') == 'pic64gx-curiosity-kit-amp' else ''}"

do_deploy:append:pic64gx-curiosity-kit-amp () {
    standalone_elf="${DEPLOY_DIR_IMAGE}/pic64gx-standalone-firmware.elf"

    if [ ! -f ${standalone_elf} ]; then
        bbfatal "Missing ${standalone_elf} from ${PIC64GX_STANDALONE_FIRMWARE_RECIPE}"
    fi

    cp ${standalone_elf} ${DEPLOYDIR}/pic64gx-standalone-firmware.elf
    sed \
        -e "s/@@AMP_DEMO@@/${PIC64GX_STANDALONE_FIRMWARE_LABEL}/g" \
        -e "s/@@AMP_PAYLOAD@@/pic64gx-standalone-firmware.elf/g" \
        -e "s/@@AMP_SKIP-AUTOBOOT@@/false/g" \
        ${WORKDIR}/amp-rust.yaml.in > ${WORKDIR}/amp-rust.yaml

    hss-payload-generator -c ${WORKDIR}/amp-rust.yaml -v ${DEPLOYDIR}/payload.bin
    cp ${DEPLOYDIR}/payload.bin ${DEPLOYDIR}/payload-${PIC64GX_STANDALONE_FIRMWARE_LABEL}.bin
    rm -f ${DEPLOYDIR}/pic64gx-standalone-firmware.elf
}
