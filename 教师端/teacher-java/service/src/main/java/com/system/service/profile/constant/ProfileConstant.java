package com.system.service.profile.constant;

import java.io.File;


public class ProfileConstant {

    public static final String IMAGE_UPLOAD_PATH = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "images";

    public static final String FILE_UPLOAD_PATH = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "files";

    public static final String RESOURCE_HANDLER = "/api/images/**";

    public static final String FILE_RESOURCE_HANDLER = "/api/file/**";

}
