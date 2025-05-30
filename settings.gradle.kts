
rootProject.name = "captcha"

include(":captchaplugin")
project(":captchaplugin").projectDir = file("plugin")

include(":captchapack")
project(":captchapack").projectDir = file("rpack")
