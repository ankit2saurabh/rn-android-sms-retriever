import React from 'react';
import { useEffect, useState } from 'react';
import { StyleSheet, View, Text, Button, Pressable, TextInput } from 'react-native';
import { getOtp, getSms, SMSRetrieverErrors } from 'rn-android-sms-retriever';

enum Tabs {
  Otp = 'Otp',
  Full_SMS = 'SMS',
}
export default function App() {
  const [activeTab, setActiveTab] = useState(Tabs.Otp);
  const [content, setContent] = useState('');
  const [reinitializeVisible, setReinitializeVisible] = useState(false);
  const [phoneNumber, onChangePhoneNumber] = useState('');

  const startReceiver = () => {
    setContent('');
    activeTab === Tabs.Otp ? startOtp() : startSms();
  };

  useEffect(() => {
    startReceiver();
  }, [activeTab]);

  const startOtp = async () => {
    setReinitializeVisible(false);
    try {
      const result = await getOtp(6, phoneNumber ? phoneNumber : null);
      setContent(result.toString());
      setReinitializeVisible(true);
    } catch (e: any) {
      if (e.toString().includes(SMSRetrieverErrors.REGEX_MISMATCH)) {
        startOtp();
      } else {
        setReinitializeVisible(true);
      }
    }
  };

  const startSms = async () => {
    setReinitializeVisible(false);
    try {
      const result = await getSms(phoneNumber ? phoneNumber : null);
      setContent(result.toString());
      setReinitializeVisible(true);
    } catch (e) {
      console.log(e);
      setReinitializeVisible(true);
    }
  };

  const Tab = ({ title, id }: { title: string; id: Tabs }) => {
    const styles = getTabsStyles({ activeTab, id });

    return (
      <Pressable
        onPress={() => {
          setActiveTab(id);
          setContent('');
        }}
        style={styles.tabPressable}
      >
        <Text style={styles.title}>{title}</Text>
      </Pressable>
    );
  };

  return (
    <View style={styles.container}>
      <View style={styles.tabsContainer}>
        <Tab title="Read OTP" id={Tabs.Otp} />
        <Tab title="Read SMS" id={Tabs.Full_SMS} />
      </View>

      <View style={styles.sectionContainer}>

        <TextInput
          style={styles.input}
          onChangeText={onChangePhoneNumber}
          value={phoneNumber}
          placeholder='Phone Number'
        />
        <Text style={styles.inputHelper}>This is the phone number that will send the OTP</Text>

        <Button title="Reinitialize Receiver" onPress={startReceiver} />
        
        <Text style={styles.title}>
          {content ? `Received ${activeTab}` : `Waiting for ${activeTab}`}
        </Text>
        <View style={styles.descriptionContainer}>
          {content && <Text style={styles.description}>{`${content}`}</Text>}
        </View>
      </View>
    </View>
  );
}

const getTabsStyles = ({ activeTab, id }: { activeTab: Tabs; id: Tabs }) =>
  StyleSheet.create({
    tabPressable: {
      borderBottomColor: '#000',
      borderBottomWidth: activeTab == id ? 1 : 0,
    },
    title: { fontSize: 32 },
  });

const styles = StyleSheet.create({
  container: { flex: 1, padding: 16 },
  tabsContainer: {
    flexDirection: 'row',
    alignItems: 'stretch',
    justifyContent: 'space-evenly',
  },
  sectionContainer: { margin: 16 },
  title: {
    fontSize: 20,
    fontWeight: '800',
    color: '#000',
    marginTop: 16,
  },
  descriptionContainer: {
    borderWidth: 1,
    borderColor: '#021',
    marginTop: 16,
    padding: 16,
    minHeight: 200,
  },
  description: {
    marginTop: 64,
    fontSize: 18,
    fontWeight: '400',
  },
  input: {
    height: 40,
    marginTop: 20,
    borderWidth: 1,
    padding: 10,
  },
  inputHelper: {
    marginTop: 8,
    marginBottom: 20,
  },
});
