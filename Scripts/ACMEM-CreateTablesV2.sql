-- MySQL Script generated by MySQL Workbench
-- Thu Nov 21 23:56:04 2024
-- Model: New Model    Version: 1.0
-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema acmemedical
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema acmemedical
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `acmemedical` DEFAULT CHARACTER SET utf8 ;
DROP USER IF EXISTS `cst8277`@`localhost`;
CREATE USER IF NOT EXISTS 'cst8277'@'localhost' IDENTIFIED BY '8277'; 
GRANT ALL ON `acmemedical`.* TO 'cst8277'@'localhost';
USE `acmemedical` ;

-- -----------------------------------------------------
-- Table `acmemedical`.`medical_school`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `acmemedical`.`medical_school` ;

CREATE TABLE IF NOT EXISTS `acmemedical`.`medical_school` (
  `school_id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `created` DATETIME NULL DEFAULT NULL,
  `updated` DATETIME NULL DEFAULT NULL,
  `version` BIGINT NOT NULL,
  PRIMARY KEY (`school_id`));


-- -----------------------------------------------------
-- Table `acmemedical`.`medical_training`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `acmemedical`.`medical_training` ;

CREATE TABLE IF NOT EXISTS `acmemedical`.`medical_training` (
  `training_id` INT NOT NULL AUTO_INCREMENT,
  `school_id` INT NOT NULL,
  `start_date` DATETIME NOT NULL,
  `end_date` DATETIME NOT NULL,
  `active` BIT(1) NOT NULL,
  `created` DATETIME NULL DEFAULT NULL,
  `updated` DATETIME NULL,
  `version` BIGINT NOT NULL,
  PRIMARY KEY (`training_id`),
  INDEX (`school_id` ASC) VISIBLE,
  CONSTRAINT ``
    FOREIGN KEY (`school_id`)
    REFERENCES `acmemedical`.`medical_school` (`school_id`));


-- -----------------------------------------------------
-- Table `acmemedical`.`medicine`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `acmemedical`.`medicine` ;

CREATE TABLE IF NOT EXISTS `acmemedical`.`medicine` (
  `medicine_id` INT NOT NULL AUTO_INCREMENT,
  `drug_name` VARCHAR(50) NOT NULL,
  `manufacturer_name` VARCHAR(50) NOT NULL,
  `dosage_information` VARCHAR(100) NOT NULL,
  `created` DATETIME NULL DEFAULT NULL,
  `updated` DATETIME NULL DEFAULT NULL,
  `version` BIGINT NOT NULL,
  PRIMARY KEY (`medicine_id`));


-- -----------------------------------------------------
-- Table `acmemedical`.`physician`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `acmemedical`.`physician` ;

CREATE TABLE IF NOT EXISTS `acmemedical`.`physician` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `first_name` VARCHAR(50) NOT NULL,
  `last_name` VARCHAR(50) NOT NULL,
  `created` DATETIME NULL DEFAULT NULL,
  `updated` DATETIME NULL DEFAULT NULL,
  `version` BIGINT NOT NULL,
  PRIMARY KEY (`id`));


-- -----------------------------------------------------
-- Table `acmemedical`.`patient`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `acmemedical`.`patient` ;

CREATE TABLE IF NOT EXISTS `acmemedical`.`patient` (
  `patient_id` INT NOT NULL AUTO_INCREMENT,
  `first_name` VARCHAR(50) NOT NULL,
  `last_name` VARCHAR(50) NOT NULL,
  `year_of_birth` INT NOT NULL,
  `home_address` VARCHAR(100) NOT NULL,
  `height_cm` INT NOT NULL,
  `weight_kg` INT NOT NULL,
  `smoker` BIT(1) NOT NULL,
  `created` DATETIME NULL DEFAULT NULL,
  `updated` DATETIME NULL DEFAULT NULL,
  `version` BIGINT NOT NULL,
  PRIMARY KEY (`patient_id`));


-- -----------------------------------------------------
-- Table `acmemedical`.`prescription`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `acmemedical`.`prescription` ;

CREATE TABLE IF NOT EXISTS `acmemedical`.`prescription` (
  `physician_id` INT NOT NULL,
  `patient_id` INT NOT NULL,
  `medicine_id` INT NULL DEFAULT NULL,
  `number_of_refills` INT NULL DEFAULT NULL,
  `prescription_information` VARCHAR(100) NULL DEFAULT NULL,
  `created` DATETIME NULL DEFAULT NULL,
  `updated` DATETIME NULL DEFAULT NULL,
  `version` BIGINT NOT NULL,
  INDEX (`physician_id` ASC) INVISIBLE,
  INDEX (`patient_id` ASC) VISIBLE,
  INDEX (`medicine_id` ASC) VISIBLE,
  PRIMARY KEY (`physician_id`, `patient_id`),
  CONSTRAINT `fk_pres_phys_id`
    FOREIGN KEY (`physician_id`)
    REFERENCES `acmemedical`.`physician` (`id`),
  CONSTRAINT `fk_pres_pati_id`
    FOREIGN KEY (`patient_id`)
    REFERENCES `acmemedical`.`patient` (`patient_id`),
  CONSTRAINT `fk_pres_medi_id`
    FOREIGN KEY (`medicine_id`)
    REFERENCES `acmemedical`.`medicine` (`medicine_id`));


-- -----------------------------------------------------
-- Table `acmemedical`.`medical_certificate`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `acmemedical`.`medical_certificate` ;

CREATE TABLE IF NOT EXISTS `acmemedical`.`medical_certificate` (
  `certificate_id` INT NOT NULL AUTO_INCREMENT,
  `physician_id` INT NOT NULL,
  `training_id` INT NULL DEFAULT NULL,
  `signed` BIT(1) NOT NULL,
  `created` DATETIME NULL DEFAULT NULL,
  `updated` DATETIME NULL DEFAULT NULL,
  `version` BIGINT NOT NULL,
  PRIMARY KEY (`certificate_id`),
  INDEX (`physician_id` ASC) VISIBLE,
  INDEX (`training_id` ASC) VISIBLE,
  CONSTRAINT `fk_cert_phys_id`
    FOREIGN KEY (`physician_id`)
    REFERENCES `acmemedical`.`physician` (`id`),
  CONSTRAINT `fk_cert_train_id`
    FOREIGN KEY (`training_id`)
    REFERENCES `acmemedical`.`medical_training` (`training_id`));


-- -----------------------------------------------------
-- Table `acmemedical`.`security_user`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `acmemedical`.`security_user` ;

CREATE TABLE IF NOT EXISTS `acmemedical`.`security_user` (
  `user_id` INT NOT NULL AUTO_INCREMENT,
  `password_hash` VARCHAR(256) NOT NULL,
  `username` VARCHAR(100) NOT NULL,
  `physician_id` INT NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE INDEX (`username` ASC) VISIBLE,
  INDEX (`physician_id` ASC) VISIBLE,
  CONSTRAINT `fk_secu_phys_id`
    FOREIGN KEY (`physician_id`)
    REFERENCES `acmemedical`.`physician` (`id`));


-- -----------------------------------------------------
-- Table `acmemedical`.`security_role`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `acmemedical`.`security_role` ;

CREATE TABLE IF NOT EXISTS `acmemedical`.`security_role` (
  `role_id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`role_id`),
  UNIQUE INDEX (`name` ASC) VISIBLE);


-- -----------------------------------------------------
-- Table `acmemedical`.`user_has_role`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `acmemedical`.`user_has_role` ;

CREATE TABLE IF NOT EXISTS `acmemedical`.`user_has_role` (
  `user_id` INT NOT NULL,
  `role_id` INT NOT NULL,
  PRIMARY KEY (`user_id`, `role_id`),
  INDEX (`user_id` ASC) VISIBLE,
  INDEX (`role_id` ASC) VISIBLE,
  CONSTRAINT `fk_uhr_user_id`
    FOREIGN KEY (`user_id`)
    REFERENCES `acmemedical`.`security_user` (`user_id`),
  CONSTRAINT `fk_uhr_role_id`
    FOREIGN KEY (`role_id`)
    REFERENCES `acmemedical`.`security_role` (`role_id`));


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
