package com.yourcompany.friendlocator.config

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import org.slf4j.LoggerFactory
import java.io.InputStream

object CloudinaryConfig {
    private val logger = LoggerFactory.getLogger(CloudinaryConfig::class.java)

    private val cloudinary: Cloudinary by lazy {
        val cloudName = System.getenv("CLOUDINARY_CLOUD_NAME") ?: "dpnzuv8mg"
        val apiKey = System.getenv("CLOUDINARY_API_KEY") ?: "851745692775944"
        val apiSecret = System.getenv("CLOUDINARY_API_SECRET") ?: "TxOkgkFhVYYmr5ByOorhxJdgbfY"

        Cloudinary(ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret,
            "secure", true
        )).also {
            logger.info("Cloudinary initialized with cloud_name: $cloudName")
        }
    }

    /**
     * 이미지 업로드
     * @param inputStream 이미지 데이터
     * @param folder 저장 폴더명
     * @return 업로드된 이미지 URL
     */
    fun uploadImage(bytes: ByteArray, folder: String = "places"): String {
        val uploadResult = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
            "folder", folder,
            "resource_type", "image",
            "transformation", ObjectUtils.asMap(
                "quality", "auto",
                "fetch_format", "auto",
                "width", 1024,
                "crop", "limit"
            )
        ))

        val url = uploadResult["secure_url"] as String
        logger.info("Image uploaded successfully: $url")
        return url
    }

    /**
     * 이미지 삭제
     * @param publicId 이미지 public_id
     */
    fun deleteImage(publicId: String) {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap())
        logger.info("Image deleted: $publicId")
    }

    /**
     * URL에서 public_id 추출
     */
    fun extractPublicId(url: String): String? {
        // https://res.cloudinary.com/xxx/image/upload/v123/folder/filename.jpg
        val regex = """/upload/(?:v\d+/)?(.+)\.\w+$""".toRegex()
        return regex.find(url)?.groupValues?.get(1)
    }
}
