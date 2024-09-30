# rn-android-sms-retriever

A react native wrapper for the SMS User Consent API to request user consent to read a single SMS verification message sent by any phone number or from a specific phone number if required. 
If the user consents, the API returns the text of the message, from which you can get the verification code and complete the verification process.

## Installation

NPM:

```sh
npm install rn-dynamic-app-icon
```

YARN:

```sh
yarn add rn-dynamic-app-icon
```

## Usage

```js
import { getOtp, getSms, SMSRetrieverErrors } from 'rn-android-sms-retriever';

// Read the next SMS
const readNextSmsRequest = async () => {
  try {
    const sms = await getSms(/*option sender's phone number*/);
  } catch (e) {
    if (e.toString().includes(SMSRetrieverErrors.CONSENT_DENIED)) {
      console.log('User denied SMS read request');
    }
  }
};

// Read OTP from SMS
const readNextOtpRequest = async (otpLength) => {
  try {
    const sms = await getOTP(otpLength, /*optional sender's phone number*/); //Can only read numeric OTP values
  } catch (e) {
    if (e.toString().includes(SMSRetrieverErrors.REGEX_MISMATCH)) {
        readNextOtpRequest(otpLength);
    }
  }
};
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Built by [true-hamid](https://github.com/true-hamid) & [mustfaibra](https://github.com/mustfaibra)
