variable "region" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "my_ip" {
  type = string
}

variable "env_name" {
  type = string
  default = "dev"
}

variable "project_name" {
  type = string
  default = "anywallet"
}