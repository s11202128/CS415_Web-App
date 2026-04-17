const axios = require('axios');

const BASE_URL = 'http://localhost:5000/api';
const TEST_EMAIL = 'testuser' + Math.floor(Math.random() * 10000) + '@example.com';
const TEST_PASSWORD = 'TestPassword123!';

async function runTests() {
  let signupOTP, loginOTP, jwtToken, transactionOTP;
  console.log('Testing signup...');
  // 1. Signup
  let res = await axios.post(BASE_URL + '/auth/signup', {
    email: TEST_EMAIL,
    password: TEST_PASSWORD
  });
  console.log('Signup:', res.data);

  // 2. Get signup OTP from DB (simulate email)
  res = await axios.get(BASE_URL + '/test/get-otp', {
    params: { email: TEST_EMAIL, type: 'signup' }
  });
  signupOTP = res.data.otp;
  console.log('Signup OTP:', signupOTP);

  // 3. Verify signup
  res = await axios.post(BASE_URL + '/auth/verify-signup', {
    email: TEST_EMAIL,
    otp: signupOTP
  });
  console.log('Verify Signup:', res.data);

  // 4. Login
  res = await axios.post(BASE_URL + '/auth/login', {
    email: TEST_EMAIL,
    password: TEST_PASSWORD
  });
  console.log('Login:', res.data);

  // 5. Get login OTP from DB
  res = await axios.get(BASE_URL + '/test/get-otp', {
    params: { email: TEST_EMAIL, type: 'login' }
  });
  loginOTP = res.data.otp;
  console.log('Login OTP:', loginOTP);

  // 6. Verify login
  res = await axios.post(BASE_URL + '/auth/verify-login', {
    email: TEST_EMAIL,
    otp: loginOTP
  });
  jwtToken = res.data.token;
  console.log('Verify Login:', res.data);

  // 7. Request transaction OTP
  res = await axios.post(BASE_URL + '/transaction/request-otp', {}, {
    headers: { Authorization: 'Bearer ' + jwtToken }
  });
  console.log('Request Transaction OTP:', res.data);

  // 8. Get transaction OTP from DB
  res = await axios.get(BASE_URL + '/test/get-otp', {
    params: { email: TEST_EMAIL, type: 'transaction' }
  });
  transactionOTP = res.data.otp;
  console.log('Transaction OTP:', transactionOTP);

  // 9. Confirm transaction
  res = await axios.post(BASE_URL + '/transaction/confirm', {
    otp: transactionOTP
  }, {
    headers: { Authorization: 'Bearer ' + jwtToken }
  });
  console.log('Confirm Transaction:', res.data);

  console.log('\nAll tests passed!');
}

runTests().catch(err => {
  if (err.response) {
    console.error('Test failed:', err.response.data);
  } else {
    console.error('Test failed:', err.message);
  }
});
