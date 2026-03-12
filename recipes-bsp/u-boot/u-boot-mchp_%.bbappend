FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append:pic64gx-curiosity-kit-amp = " file://amp-rust.yaml.in"

do_deploy[depends] += "${@' pic64gx-zephyr-standalone:do_deploy' if d.getVar('MACHINE') == 'pic64gx-curiosity-kit-amp' else ''}"

do_deploy:append:pic64gx-curiosity-kit-amp () {
    if [ ! -f ${DEPLOY_DIR_IMAGE}/zephyr-amp-application.elf ]; then
        bbfatal "Missing ${DEPLOY_DIR_IMAGE}/zephyr-amp-application.elf from pic64gx-zephyr-standalone"
    fi

    cp ${DEPLOY_DIR_IMAGE}/zephyr-amp-application.elf ${DEPLOYDIR}/zephyr-amp-application.elf
    sed \
        -e "s/@@AMP_DEMO@@/${PIC64GX_ZEPHYR_APP}/g" \
        -e "s/@@AMP_PAYLOAD@@/zephyr-amp-application.elf/g" \
        -e "s/@@AMP_SKIP-AUTOBOOT@@/false/g" \
        ${WORKDIR}/amp-rust.yaml.in > ${WORKDIR}/amp-rust.yaml

    hss-payload-generator -c ${WORKDIR}/amp-rust.yaml -v ${DEPLOYDIR}/payload.bin
    cp ${DEPLOYDIR}/payload.bin ${DEPLOYDIR}/payload-${PIC64GX_ZEPHYR_APP}.bin
    rm -f ${DEPLOYDIR}/zephyr-amp-application.elf
}
