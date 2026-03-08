require recipes-core/images/core-image-minimal.bb
IMAGE_INSTALL:append = " helloworld hellorust gpioblink"
IMAGE_INSTALL:remove = "packagegroup-mchp-apps-amp"
