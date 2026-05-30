const _api_domain = ""
const _api_path = "/api"
const _api_version = "v0"
const _base_url = `${_api_domain}${_api_path}/${_api_version}`
const _identity = {
    public: "public",
    student: "student",
    teacher: "teacher",
    admin: "admin"
}

const _public_url = `${_base_url}/${_identity.public}`
const _student_url = `${_base_url}/${_identity.student}`
const _teacher_url = `${_base_url}/${_identity.teacher}`
const _admin_url = `${_base_url}/${_identity.admin}`

const api = {
    public: {
        challenge:{
            captcha: `${_public_url}/challenge/captcha`,
            token: `${_public_url}/challenge/token`
        }
    },
    student: {
        auth: {
            token: `${_student_url}/auth/token`,
            refreshToken: `${_student_url}/auth/token/refresh`,
            verifyToken: `${_student_url}/auth/vtoken`
        },
        user: {
            register: `${_student_url}/users`,
        }
    }
}

export default api;