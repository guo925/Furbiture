// 测试登录流程
const axios = require('axios');

async function testLogin() {
  try {
    const response = await axios.post('http://localhost:9090/api/auth/login', {
      username: 'admin',
      password: '123456'
    }, {
      headers: {
        'Content-Type': 'application/json'
      }
    });

    console.log('Response status:', response.status);
    console.log('Response data:', JSON.stringify(response.data, null, 2));
    console.log('Response data.data:', JSON.stringify(response.data.data, null, 2));
    console.log('Has token:', !!response.data.data.token);
    console.log('Has user:', !!response.data.data.user);
  } catch (error) {
    console.error('Error:', error.message);
    if (error.response) {
      console.error('Response data:', error.response.data);
      console.error('Response status:', error.response.status);
    }
  }
}

testLogin();