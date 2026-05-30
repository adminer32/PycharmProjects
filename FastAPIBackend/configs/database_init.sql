/*
SQLyog Trial v13.1.8 (64 bit)
MySQL - 9.1.0 : Database - aiidecn
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`aiidecn` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `aiidecn`;

/*Table structure for table `jwt_blacklist` */

DROP TABLE IF EXISTS `jwt_blacklist`;

CREATE TABLE `jwt_blacklist` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `token_id` char(64) NOT NULL,
  `access_token` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `refresh_token` text NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `token_id` (`token_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Data for the table `jwt_blacklist` */

/*Table structure for table `jwt_ip` */

DROP TABLE IF EXISTS `jwt_ip`;

CREATE TABLE `jwt_ip` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `token_id` char(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `token_id` (`token_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Data for the table `jwt_ip` */

insert  into `jwt_ip`(`id`,`token_id`,`ip_address`,`created_at`) values 
(1,'b78c0facf8451b4b850a3de234b0b9e881acb28940103b5cc8807a8a1b976456','127.0.0.1','2026-03-07 21:58:44'),
(2,'0028cf55de16fdf8b8b43a1152f8a8aa47ae6dd47e6da14ecd35cf3680a9a8aa','127.0.0.1','2026-03-19 21:59:58');

/*Table structure for table `student_profile` */

DROP TABLE IF EXISTS `student_profile`;

CREATE TABLE `student_profile` (
  `user_id` bigint unsigned NOT NULL,
  `student_id` bigint unsigned NOT NULL,
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `gender` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '未知',
  `grade` bigint unsigned NOT NULL,
  `major` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `class_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_student_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*Data for the table `student_profile` */

/*Table structure for table `users` */

DROP TABLE IF EXISTS `users`;

CREATE TABLE `users` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password_hash` char(128) NOT NULL,
  `password_salt` char(16) NOT NULL,
  `role` enum('ADMIN','TEACHER','STUDENT') NOT NULL DEFAULT 'STUDENT',
  `active` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/*Data for the table `users` */

insert  into `users`(`id`,`username`,`password_hash`,`password_salt`,`role`,`active`,`created_at`,`updated_at`) values 
(1,'test01','e467d3a400984927014c731dd93fe09f6c1d8bf4896d806a6927c7c76db38b0428aea599a048197dc2a5c14faaf7a9f34c14fc267e4aafb38da7363bf6a7f30e','','STUDENT',1,'2026-03-05 15:59:06','2026-03-05 20:31:51');

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
