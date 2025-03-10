package com.nshm.attendancesystem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response

class AttendanceViewModel : ViewModel() {

    private val _attendanceService = attendanceService

    private val _name = MutableStateFlow("")

    val name: StateFlow<String> get() = _name

    var messageScan by mutableStateOf("")
        private set

    var color by mutableStateOf("")
        private set

    var registeredStudentsList by mutableStateOf<List<User>>(emptyList())
        private set

    var message by mutableStateOf("")
        private set

    var response by mutableStateOf("")

    var mailMessage by mutableStateOf("")

    init {
        fetchStudentsList()
    }

    fun fetchScan(id: String) {
        viewModelScope.launch {
            try {
                val attendanceData = _attendanceService.getScanQr(id)
                formatData(attendanceData)
            } catch (_: Exception) {
            }
        }
    }

    fun updateUserInfo(id: String,user: User) {
        viewModelScope.launch {
            try {
                val response = _attendanceService.updateUserInfo(id = id, userInfo = user)
            } catch (_: Exception) {
            }
        }
    }



    private fun formatData(attendanceData: Response<ScanResponse>) {
        _name.value = attendanceData.body()?.user?.name ?: "User Not Found"
        message = attendanceData.body()?.message ?: "User Not Found"
        if (message == "User checked in successfully") {
            messageScan = "Authorized"
            color = "green"
        } else if (message == "Duplicate entry") {
            messageScan = "Duplicate Scan"
            color = "yellow"
        } else {
            messageScan = ""
            color = "red"
        }
    }

    fun fetchStudentsList() {
        viewModelScope.launch {
            try {
                val registeredStudents = _attendanceService.getRegisteredStudents()
                registeredStudentsList = registeredStudents
            } catch (_: Exception) {
                fetchStudentsList()
            }
        }
    }

    fun sendMail(
        collegeId: String
    ) {
        viewModelScope.launch {
            mailMessage = try {
                val response = _attendanceService.sendMail(
                    collegeId = SendMailBody(
                        collegeId = collegeId.toLong()
                    )
                )
                if (response.code() == 200) {
                    "Email Sent Successfully"
                } else {
                    "Failed to Send Mail"
                }
            } catch (e: Exception) {
                "Failed to Send Mail"
            }
        }
    }

    fun registerUser(
        name: String,
        collegeEmail: String,
        collegeId: Long,
        year: String,
        department: String,
        contactNumber: Long,
        whatsappNumber: Long
    ) {
        val userRegistration = RegistrationData(
            name = name,
            collegeEmail = collegeEmail,
            collegeId = collegeId,
            year = year,
            department = department,
            contactNumber = contactNumber,
            whatsappNumber = whatsappNumber
        )
        viewModelScope.launch {
            try {
                val response1 = attendanceService.registerUser(userRegistration)
                response = response1.message
            } catch (e: HttpException) {
                if (e.code() == 400) {
                    val errorBody = e.response()?.errorBody()?.string()

                    response = errorBody?.let {
                        try {
                            JSONObject(it).getString("message")
                        } catch (jsonException: Exception) {
                            "Registration Failed"
                        }
                    } ?: "Registration Failed"
                } else {
                    response = "Registration Failed with Error: ${e.code()}"
                }
            } catch (e: Exception) {
                response = "Registration Failed: ${e.localizedMessage}"
            }
        }
    }
}